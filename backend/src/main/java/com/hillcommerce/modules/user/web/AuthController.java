package com.hillcommerce.modules.user.web;

import static com.hillcommerce.modules.user.web.AuthDtos.AuthUserResponse;
import static com.hillcommerce.modules.user.web.AuthDtos.LoginRequest;
import static com.hillcommerce.modules.user.web.AuthDtos.RegisterRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hillcommerce.modules.user.model.AuthUser;
import com.hillcommerce.modules.user.security.AuthenticatedUserPrincipal;
import com.hillcommerce.modules.logging.service.LoggingService;
import com.hillcommerce.modules.user.service.UserAccountService;

/**
 * 认证 REST 控制器，暴露 /api/auth/* 端点。
 *
 * 登录流程手动管理 SecurityContext 而非依赖 Spring Security 的 AbstractAuthenticationProcessingFilter，
 * 以便在同一个控制器方法中完成认证、日志记录和响应构建。
 *
 * 已知问题：login 中未调用 request.changeSessionId()，session fixation 防护不完整（见 review-notes）。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserAccountService userAccountService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final LoggingService loggingService;

    public AuthController(
        UserAccountService userAccountService,
        AuthenticationManager authenticationManager,
        SecurityContextRepository securityContextRepository,
        LoggingService loggingService
    ) {
        this.userAccountService = userAccountService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.loggingService = loggingService;
    }

    /** 注册成功后返回 201 + 用户信息，前端自行跳转至登录页完成首次登录 */
    @PostMapping("/register")
    public ResponseEntity<AuthUserResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthUser authUser = userAccountService.register(request.email(), request.password(), request.nickname());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(authUser));
    }

    /**
     * 登录：认证 → 手动构建 SecurityContext → 持久化到 HttpSession → 返回用户信息。
     * 认证失败时记录 warn 日志（不记录明文密码），然后原样向上抛出 AuthenticationException，
     * 由 SecurityConfig 的 jsonAuthenticationEntryPoint 返回 401 JSON。
     */
    @PostMapping("/login")
    public AuthUserResponse login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse
    ) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        }
        catch (AuthenticationException exception) {
            recordLoginSafely(
                null,
                request.email(),
                "UNKNOWN",
                "FAILURE",
                resolveRemoteAddr(httpServletRequest),
                httpServletRequest.getHeader("User-Agent"));
            log.warn(
                "Login failed: email={}, remoteAddr={}, reason={}",
                request.email(),
                resolveRemoteAddr(httpServletRequest),
                sanitizeReason(exception));
            throw exception;
        }

        // 手动创建并持久化 SecurityContext——绕过 UsernamePasswordAuthenticationFilter 的默认流程
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(context, httpServletRequest, httpServletResponse);

        AuthenticatedUserPrincipal principal = (AuthenticatedUserPrincipal) authentication.getPrincipal();
        userAccountService.recordSuccessfulLogin(principal.id());
        log.info(
            "Login succeeded: email={}, userId={}, roles={}, remoteAddr={}",
            principal.email(),
            principal.id(),
            principal.roles(),
            resolveRemoteAddr(httpServletRequest));
        recordLoginSafely(
            principal.id(),
            principal.email(),
            String.join(",", principal.roles()),
            "SUCCESS",
            resolveRemoteAddr(httpServletRequest),
            httpServletRequest.getHeader("User-Agent"));

        return toResponse(principal);
    }

    /**
     * 返回当前会话的已认证用户信息。
     * authentication 参数由 Spring Security 从 SecurityContextHolder 自动注入。
     */
    @GetMapping("/me")
    public AuthUserResponse me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new AuthenticationCredentialsNotFoundException("No authenticated user");
        }
        return toResponse(principal);
    }

    /**
     * 注销：销毁 HttpSession + 清理 SecurityContextHolder。
     * 前端 logout 代理同步清除 JSESSIONID Cookie（set-cookie expires=0）。
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    private AuthUserResponse toResponse(AuthUser authUser) {
        return new AuthUserResponse(authUser.email(), authUser.nickname(), authUser.roles());
    }

    private AuthUserResponse toResponse(AuthenticatedUserPrincipal principal) {
        return new AuthUserResponse(principal.email(), principal.nickname(), principal.roles());
    }

    /**
     * 解析客户端真实 IP：优先取 X-Forwarded-For 最左侧地址（Nginx 代理场景），
     * 回退到 request.getRemoteAddr()。
     */
    private String resolveRemoteAddr(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 将 AuthenticationException 归类为安全的日志标签，避免异常类名或消息泄露内部细节。
     */
    private String sanitizeReason(AuthenticationException exception) {
        if (exception instanceof BadCredentialsException) {
            return "bad-credentials";
        }
        if (exception instanceof AuthenticationServiceException) {
            return "authentication-service-error";
        }
        return exception.getClass().getSimpleName();
    }

    private void recordLoginSafely(
        Long userId,
        String email,
        String roleSnapshot,
        String loginResult,
        String ipAddress,
        String userAgent
    ) {
        try {
            loggingService.recordLogin(userId, email, roleSnapshot, loginResult, ipAddress, userAgent);
        } catch (Exception exception) {
            log.error("Failed to persist login log: email={}, result={}", email, loginResult, exception);
        }
    }
}

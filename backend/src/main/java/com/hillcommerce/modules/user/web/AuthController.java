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
import com.hillcommerce.modules.user.security.AppUserPrincipal;
import com.hillcommerce.modules.user.service.UserAccountService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserAccountService userAccountService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(
        UserAccountService userAccountService,
        AuthenticationManager authenticationManager,
        SecurityContextRepository securityContextRepository
    ) {
        this.userAccountService = userAccountService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthUserResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthUser authUser = userAccountService.register(request.email(), request.password(), request.nickname());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(authUser));
    }

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
            log.warn(
                "Login failed: email={}, remoteAddr={}, reason={}",
                request.email(),
                resolveRemoteAddr(httpServletRequest),
                sanitizeReason(exception));
            throw exception;
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(context, httpServletRequest, httpServletResponse);

        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        log.info(
            "Login succeeded: email={}, userId={}, roles={}, remoteAddr={}",
            principal.email(),
            principal.id(),
            principal.roles(),
            resolveRemoteAddr(httpServletRequest));

        return toResponse(principal);
    }

    @GetMapping("/me")
    public AuthUserResponse me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new AuthenticationCredentialsNotFoundException("No authenticated user");
        }
        return toResponse(principal);
    }

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

    private AuthUserResponse toResponse(AppUserPrincipal principal) {
        return new AuthUserResponse(principal.email(), principal.nickname(), principal.roles());
    }

    private String resolveRemoteAddr(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String sanitizeReason(AuthenticationException exception) {
        if (exception instanceof BadCredentialsException) {
            return "bad-credentials";
        }
        if (exception instanceof AuthenticationServiceException) {
            return "authentication-service-error";
        }
        return exception.getClass().getSimpleName();
    }
}

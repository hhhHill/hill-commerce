package com.hillcommerce.modules.user.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import com.hillcommerce.modules.user.service.PasswordService;

/**
 * 自定义 AuthenticationProvider，接管邮箱+密码认证。
 *
 * 替代 DaoAuthenticationProvider，原因：
 *   1. username 参数实际是 email，UsernameNotFoundException 需要统一处理
 *   2. PasswordService 统一封装 BCrypt 密码校验策略
 *   3. 认证成功后的 UsernamePasswordAuthenticationToken 擦除 credentials（设为 null），
 *      避免明文密码在 SecurityContext 中残留
 *
 * loadUserByUsername 若抛 UsernameNotFoundException，会由 ProviderManager 向上传播，
 * AuthController 的 catch 块将其归类为认证失败。
 */
@Component
public class AppAuthenticationProvider implements AuthenticationProvider {

    private final AppUserDetailsService userDetailsService;
    private final PasswordService passwordService;

    public AppAuthenticationProvider(AppUserDetailsService userDetailsService, PasswordService passwordService) {
        this.userDetailsService = userDetailsService;
        this.passwordService = passwordService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        // getCredentials() 为 null 时 String.valueOf 返回 "null"，不会匹配任何哈希，安全无影响
        String rawPassword = String.valueOf(authentication.getCredentials());

        AppUserPrincipal principal = (AppUserPrincipal) userDetailsService.loadUserByUsername(email);
        if (!principal.isEnabled()) {
            throw new DisabledException("User is disabled");
        }
        if (!passwordService.matches(rawPassword, principal.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        SessionUserPrincipal sessionPrincipal = SessionUserPrincipal.from(principal);

        // credentials 设为 null，principal 改为不含 passwordHash 的会话对象
        return UsernamePasswordAuthenticationToken.authenticated(
            sessionPrincipal,
            null,
            sessionPrincipal.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

package com.hillcommerce.modules.user.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import com.hillcommerce.modules.user.service.PasswordService;

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
        String rawPassword = String.valueOf(authentication.getCredentials());

        AppUserPrincipal principal = (AppUserPrincipal) userDetailsService.loadUserByUsername(email);
        if (!passwordService.matches(rawPassword, principal.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return UsernamePasswordAuthenticationToken.authenticated(
            principal,
            null,
            principal.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

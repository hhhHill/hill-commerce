package com.hillcommerce.modules.user.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.hillcommerce.modules.user.model.AuthUser;

public class AppUserPrincipal implements UserDetails {

    private final AuthUser authUser;
    private final List<SimpleGrantedAuthority> authorities;

    public AppUserPrincipal(AuthUser authUser) {
        this.authUser = authUser;
        this.authorities = authUser.roles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .toList();
    }

    public Long id() {
        return authUser.id();
    }

    public String email() {
        return authUser.email();
    }

    public String nickname() {
        return authUser.nickname();
    }

    public List<String> roles() {
        return authUser.roles();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return authUser.passwordHash();
    }

    @Override
    public String getUsername() {
        return authUser.email();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(authUser.status());
    }
}

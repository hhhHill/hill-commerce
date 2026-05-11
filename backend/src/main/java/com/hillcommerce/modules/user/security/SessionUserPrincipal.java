package com.hillcommerce.modules.user.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 持久化到 HTTP Session 的 principal。
 *
 * 仅保留会话期所需的身份字段与角色，不保留 passwordHash。
 */
public class SessionUserPrincipal implements UserDetails, AuthenticatedUserPrincipal {

    private final Long id;
    private final String email;
    private final String nickname;
    private final String status;
    private final List<String> roles;
    private final List<SimpleGrantedAuthority> authorities;

    public SessionUserPrincipal(Long id, String email, String nickname, String status, List<String> roles) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.status = status;
        this.roles = List.copyOf(roles);
        this.authorities = this.roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .toList();
    }

    public static SessionUserPrincipal from(AppUserPrincipal principal) {
        return new SessionUserPrincipal(
            principal.id(),
            principal.email(),
            principal.nickname(),
            principal.status(),
            principal.roles());
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public String email() {
        return email;
    }

    @Override
    public String nickname() {
        return nickname;
    }

    @Override
    public List<String> roles() {
        return roles;
    }

    public String status() {
        return status;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
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
        return "ACTIVE".equals(status);
    }
}

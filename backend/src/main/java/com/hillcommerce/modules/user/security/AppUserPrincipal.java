package com.hillcommerce.modules.user.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.hillcommerce.modules.user.model.AuthUser;

/**
 * 自定义 UserDetails 实现，将 AuthUser 适配为 Spring Security 的 Principal。
 *
 * 角色编码（CUSTOMER/SALES/ADMIN）在构造时转换为 ROLE_ 前缀的 SimpleGrantedAuthority，
 * 供 SecurityConfig 的 hasRole/hasAnyRole 使用。
 *
 * getPassword() 返回认证阶段所需的 passwordHash（BCrypt 哈希）。
 * 认证成功后应转换为不含 passwordHash 的 SessionUserPrincipal 再写入 HTTP Session。
 * isEnabled() 由 users.status='ACTIVE' 驱动，MVP 阶段账户锁定/过期/凭据过期均不启用。
 */
public class AppUserPrincipal implements UserDetails, AuthenticatedUserPrincipal {

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

    public String status() {
        return authUser.status();
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

    /** MVP 阶段不启用账户过期机制 */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /** MVP 阶段不启用账户锁定机制 */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /** MVP 阶段不启用凭据过期机制 */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** 账户启用状态由 users.status 字段驱动 */
    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(authUser.status());
    }
}

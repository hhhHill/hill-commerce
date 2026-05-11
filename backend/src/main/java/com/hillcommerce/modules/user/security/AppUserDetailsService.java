package com.hillcommerce.modules.user.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.hillcommerce.modules.user.model.AuthUser;
import com.hillcommerce.modules.user.service.UserAccountService;

/**
 * Spring Security UserDetailsService 适配层。
 *
 * 将 UserAccountService.loadByEmail（返回 null 表示不存在）的结果
 * 转换为 Spring Security 的 UsernameNotFoundException。
 * username 参数实际上是 email，对应统一账号体系的设计。
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserAccountService userAccountService;

    public AppUserDetailsService(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AuthUser authUser = userAccountService.loadByEmail(username);
        if (authUser == null) {
            throw new UsernameNotFoundException("User not found");
        }
        return new AppUserPrincipal(authUser);
    }
}

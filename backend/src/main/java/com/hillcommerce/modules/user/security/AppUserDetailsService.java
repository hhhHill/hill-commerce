package com.hillcommerce.modules.user.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.hillcommerce.modules.user.model.AuthUser;
import com.hillcommerce.modules.user.service.UserAccountService;

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

package com.hillcommerce.backend.config;

import com.hillcommerce.auth.application.AuthApplicationService;
import com.hillcommerce.common.security.jwt.JwtTokenService;
import com.hillcommerce.user.application.UserApplicationService;
import com.hillcommerce.user.infrastructure.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModuleConfiguration {

    @Bean
    public UserRepository userRepository() {
        return new UserRepository();
    }

    @Bean
    public UserApplicationService userApplicationService(UserRepository userRepository) {
        UserApplicationService service = new UserApplicationService(userRepository);
        service.register(new com.hillcommerce.user.api.command.RegisterUserCommand("alice", "password", "13800000000"));
        return service;
    }

    @Bean
    public JwtTokenService jwtTokenService() {
        return new JwtTokenService("mvp-secret");
    }

    @Bean
    public AuthApplicationService authApplicationService(UserApplicationService userApplicationService,
                                                         JwtTokenService jwtTokenService) {
        return new AuthApplicationService(userApplicationService, jwtTokenService);
    }
}

package com.hillcommerce.auth.application;

import com.hillcommerce.auth.api.AuthFacade;
import com.hillcommerce.auth.api.command.LoginCommand;
import com.hillcommerce.auth.domain.AuthToken;
import com.hillcommerce.common.security.jwt.JwtTokenService;
import com.hillcommerce.user.api.UserFacade;
import com.hillcommerce.user.domain.User;

public class AuthApplicationService implements AuthFacade {

    private final UserFacade userFacade;
    private final JwtTokenService jwtTokenService;

    public AuthApplicationService(UserFacade userFacade, JwtTokenService jwtTokenService) {
        this.userFacade = userFacade;
        this.jwtTokenService = jwtTokenService;
    }

    public static AuthApplicationService stub() {
        UserFacade stubUserFacade = new UserFacade() {
            @Override
            public User register(com.hillcommerce.user.api.command.RegisterUserCommand command) {
                return new User(1L, command.username(), command.phone());
            }

            @Override
            public User findByUsername(String username) {
                return new User(1L, username, "13800000000");
            }
        };

        return new AuthApplicationService(stubUserFacade, new JwtTokenService("test-secret"));
    }

    @Override
    public AuthToken login(LoginCommand command) {
        User user = userFacade.findByUsername(command.username());
        return new AuthToken(jwtTokenService.issueToken(user.id(), user.username(), "CUSTOMER"));
    }
}

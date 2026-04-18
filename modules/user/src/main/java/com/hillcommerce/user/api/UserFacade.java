package com.hillcommerce.user.api;

import com.hillcommerce.user.api.command.RegisterUserCommand;
import com.hillcommerce.user.domain.User;

public interface UserFacade {

    User register(RegisterUserCommand command);

    User findByUsername(String username);
}

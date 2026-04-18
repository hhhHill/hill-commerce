package com.hillcommerce.auth.api;

import com.hillcommerce.auth.api.command.LoginCommand;
import com.hillcommerce.auth.domain.AuthToken;

public interface AuthFacade {

    AuthToken login(LoginCommand command);
}

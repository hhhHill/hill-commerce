package com.hillcommerce.common.security.context;

import com.hillcommerce.common.core.context.CurrentUser;
import com.hillcommerce.common.core.context.CurrentUserProvider;

public class RequestCurrentUserProvider implements CurrentUserProvider {

    @Override
    public CurrentUser getCurrentUser() {
        return new CurrentUser(1L, "system", "ADMIN");
    }
}

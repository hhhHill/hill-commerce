package com.hillcommerce.common.core.context;

@FunctionalInterface
public interface CurrentUserProvider {

    CurrentUser getCurrentUser();
}

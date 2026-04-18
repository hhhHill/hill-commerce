package com.hillcommerce.common.core.context;

public record CurrentUser(Long userId, String username, String role) {
}

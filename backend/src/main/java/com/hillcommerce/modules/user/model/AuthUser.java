package com.hillcommerce.modules.user.model;

import java.util.List;

public record AuthUser(
    Long id,
    String email,
    String passwordHash,
    String nickname,
    String status,
    List<String> roles
) {
}

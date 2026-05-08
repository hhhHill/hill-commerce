package com.hillcommerce.modules.user.web;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 64) String password,
        @NotBlank @Size(max = 64) String nickname
    ) {
    }

    public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password
    ) {
    }

    public record AuthUserResponse(
        String email,
        String nickname,
        List<String> roles
    ) {
    }
}

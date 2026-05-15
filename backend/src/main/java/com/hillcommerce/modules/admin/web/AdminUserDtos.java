package com.hillcommerce.modules.admin.web;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AdminUserDtos {

    private AdminUserDtos() {
    }

    public record SalesUserResponse(
        Long id,
        String email,
        String nickname,
        boolean enabled,
        LocalDateTime createdAt
    ) {
    }

    public record SalesUserListResponse(
        List<SalesUserResponse> users
    ) {
    }

    public record CreateSalesRequest(
        @NotBlank @Email @Size(max = 128) String email,
        @NotBlank @Size(max = 64) String nickname,
        @NotBlank @Size(min = 6, max = 255) String password
    ) {
    }

    public record ResetPasswordRequest(
        @NotBlank @Size(min = 6, max = 255) String password
    ) {
    }

    public record UserActionResponse(
        Long userId,
        boolean enabled
    ) {
    }
}

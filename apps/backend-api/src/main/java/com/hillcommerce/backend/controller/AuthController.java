package com.hillcommerce.backend.controller;

import com.hillcommerce.auth.application.AuthApplicationService;
import com.hillcommerce.auth.api.command.LoginCommand;
import com.hillcommerce.auth.domain.AuthToken;
import com.hillcommerce.common.core.api.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthToken> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(authApplicationService.login(new LoginCommand(request.username(), request.password())));
    }

    public record LoginRequest(String username, String password) {
    }
}

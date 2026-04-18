package com.hillcommerce.backend.controller;

import com.hillcommerce.common.core.api.ApiResponse;
import com.hillcommerce.user.application.UserApplicationService;
import com.hillcommerce.user.domain.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserApplicationService userApplicationService;

    public UserController(UserApplicationService userApplicationService) {
        this.userApplicationService = userApplicationService;
    }

    @GetMapping("/{username}")
    public ApiResponse<User> findByUsername(@PathVariable String username) {
        return ApiResponse.success(userApplicationService.findByUsername(username));
    }
}

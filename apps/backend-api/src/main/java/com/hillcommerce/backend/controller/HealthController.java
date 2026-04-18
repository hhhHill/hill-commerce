package com.hillcommerce.backend.controller;

import com.hillcommerce.common.core.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ApiResponse<HealthStatus> health() {
        return ApiResponse.success(new HealthStatus("UP"));
    }

    public record HealthStatus(String status) {
    }
}

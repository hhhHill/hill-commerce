package com.hillcommerce.modules.admin.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("message", "admin-access-granted");
    }
}

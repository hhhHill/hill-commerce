package com.hillcommerce.modules.admin.web;

import static com.hillcommerce.modules.admin.web.AdminDashboardDtos.DashboardSummaryResponse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hillcommerce.modules.admin.service.AdminDashboardService;
import com.hillcommerce.modules.user.security.AuthenticatedUserPrincipal;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse summary(Authentication authentication) {
        requireAdmin(authentication);
        return adminDashboardService.getSummary();
    }

    private void requireAdmin(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new AccessDeniedException("forbidden");
        }
        if (!principal.roles().contains("ADMIN")) {
            throw new AccessDeniedException("forbidden");
        }
    }
}

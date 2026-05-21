package com.hillcommerce.modules.admin.web;

import static com.hillcommerce.modules.admin.dto.AdminDashboardDtos.DashboardSummaryResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hillcommerce.framework.security.RequireRole;
import com.hillcommerce.modules.admin.context.ShopContext;
import com.hillcommerce.modules.admin.service.AdminDashboardService;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/summary")
    @RequireRole({"ADMIN", "MERCHANT"})
    public DashboardSummaryResponse summary() {
        Long shopId = ShopContext.currentShopId();
        return adminDashboardService.getSummary(shopId);
    }
}

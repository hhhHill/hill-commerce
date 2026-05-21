package com.hillcommerce.modules.admin.web;

import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.AggregateProfileResponse;
import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.AnomalyItem;
import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.AnomalyStatusResponse;
import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.ProductRankingResponse;
import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.TrendResponse;
import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.UserProfileDetail;
import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.UserProfileSummary;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hillcommerce.framework.security.RequireRole;
import com.hillcommerce.modules.admin.context.ShopContext;
import com.hillcommerce.modules.admin.service.AnomalyDetectionService;
import com.hillcommerce.modules.admin.service.ProductRankingService;
import com.hillcommerce.modules.admin.service.SalesTrendService;
import com.hillcommerce.modules.admin.service.UserProfileService;

@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    private final SalesTrendService salesTrendService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final ProductRankingService productRankingService;
    private final UserProfileService userProfileService;

    public AdminAnalyticsController(
        SalesTrendService salesTrendService,
        AnomalyDetectionService anomalyDetectionService,
        ProductRankingService productRankingService,
        UserProfileService userProfileService
    ) {
        this.salesTrendService = salesTrendService;
        this.anomalyDetectionService = anomalyDetectionService;
        this.productRankingService = productRankingService;
        this.userProfileService = userProfileService;
    }

    @GetMapping("/trends")
    @RequireRole({"ADMIN", "MERCHANT"})
    public TrendResponse trends(
        @RequestParam(defaultValue = "day") String granularity,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return salesTrendService.getTrends(granularity, from, to, ShopContext.currentShopId());
    }

    @GetMapping("/anomalies")
    @RequireRole({"ADMIN", "MERCHANT"})
    public List<AnomalyItem> anomalies() {
        return anomalyDetectionService.currentAnomalies();
    }

    @GetMapping("/anomalies/status")
    @RequireRole({"ADMIN", "MERCHANT"})
    public AnomalyStatusResponse anomalyStatus() {
        return anomalyDetectionService.getStatus();
    }

    @PostMapping("/anomalies/{id}/acknowledge")
    @RequireRole("ADMIN")
    public AnomalyStatusResponse acknowledge(@PathVariable String id) {
        anomalyDetectionService.acknowledge(id);
        return anomalyDetectionService.getStatus();
    }

    @GetMapping("/rankings/products")
    @RequireRole({"ADMIN", "MERCHANT"})
    public ProductRankingResponse productRankings(
        @RequestParam(defaultValue = "today") String range,
        @RequestParam(defaultValue = "10") int limit
    ) {
        return productRankingService.getRankings(range, limit, ShopContext.currentShopId());
    }

    @GetMapping("/profiles/aggregate")
    @RequireRole("ADMIN")
    public AggregateProfileResponse aggregateProfiles() {
        return userProfileService.getAggregateProfiles();
    }

    @GetMapping("/profiles/users/search")
    @RequireRole("ADMIN")
    public List<UserProfileSummary> searchUsers(@RequestParam String keyword) {
        return userProfileService.searchUsers(keyword);
    }

    @GetMapping("/profiles/users/{userId}")
    @RequireRole({"ADMIN", "MERCHANT"})
    public UserProfileDetail userProfile(@PathVariable Long userId) {
        return userProfileService.getUserProfile(userId);
    }
}

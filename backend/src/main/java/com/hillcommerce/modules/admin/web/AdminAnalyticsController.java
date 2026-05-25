package com.hillcommerce.modules.admin.web;

import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.AggregateProfileResponse;
import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.AnomalyListResponse;
import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.AnomalyStatusResponse;
import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.ProductRankingResponse;
import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.TrendResponse;
import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.UserProfileDetail;
import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.UserProfileSummary;
import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.TodaySnapshotResponse;
import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.ProductFunnelResponse;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.security.core.Authentication;

import com.hillcommerce.framework.security.RequireRole;
import com.hillcommerce.modules.admin.context.ShopContext;
import com.hillcommerce.modules.admin.service.AnomalyDetectionService;
import com.hillcommerce.modules.admin.service.ProductRankingService;
import com.hillcommerce.modules.admin.service.SalesTrendService;
import com.hillcommerce.modules.admin.service.UserProfileService;
import com.hillcommerce.modules.admin.service.TodaySnapshotService;
import com.hillcommerce.modules.admin.service.ProductFunnelService;

@Validated
@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    private final SalesTrendService salesTrendService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final ProductRankingService productRankingService;
    private final UserProfileService userProfileService;
    private final TodaySnapshotService todaySnapshotService;
    private final ProductFunnelService productFunnelService;

    public AdminAnalyticsController(
        SalesTrendService salesTrendService,
        AnomalyDetectionService anomalyDetectionService,
        ProductRankingService productRankingService,
        UserProfileService userProfileService,
        TodaySnapshotService todaySnapshotService,
        ProductFunnelService productFunnelService
    ) {
        this.salesTrendService = salesTrendService;
        this.anomalyDetectionService = anomalyDetectionService;
        this.productRankingService = productRankingService;
        this.userProfileService = userProfileService;
        this.todaySnapshotService = todaySnapshotService;
        this.productFunnelService = productFunnelService;
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
    public AnomalyListResponse anomalies(
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        long shopId = ShopContext.currentShopId() != null ? ShopContext.currentShopId() : 0L;
        return anomalyDetectionService.currentAnomalies(shopId, page, size);
    }

    @GetMapping("/anomalies/status")
    @RequireRole({"ADMIN", "MERCHANT"})
    public AnomalyStatusResponse anomalyStatus() {
        long shopId = ShopContext.currentShopId() != null ? ShopContext.currentShopId() : 0L;
        return anomalyDetectionService.getStatus(shopId);
    }

    @PostMapping("/anomalies/{id}/acknowledge")
    @RequireRole("ADMIN")
    public AnomalyStatusResponse acknowledge(@PathVariable long id, Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        anomalyDetectionService.acknowledge(id, operator);
        long shopId = ShopContext.currentShopId() != null ? ShopContext.currentShopId() : 0L;
        return anomalyDetectionService.getStatus(shopId);
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

    @GetMapping("/today")
    @RequireRole({"ADMIN", "MERCHANT"})
    public TodaySnapshotResponse today() {
        long shopId = ShopContext.currentShopId() != null ? ShopContext.currentShopId() : 0L;
        return todaySnapshotService.getToday(shopId);
    }

    @GetMapping("/funnel")
    @RequireRole({"ADMIN", "MERCHANT"})
    public ProductFunnelResponse funnel(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit
    ) {
        long shopId = ShopContext.currentShopId() != null ? ShopContext.currentShopId() : 0L;
        return productFunnelService.getFunnel(from, to, limit, shopId);
    }
}

package com.hillcommerce.modules.admin.web;

import static com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.AggregateProfileResponse;
import static com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.AnomalyItem;
import static com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.AnomalyStatusResponse;
import static com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.ProductRankingResponse;
import static com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.TrendResponse;
import static com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.UserProfileDetail;
import static com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.UserProfileSummary;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hillcommerce.modules.admin.service.AnomalyDetectionService;
import com.hillcommerce.modules.admin.service.ProductRankingService;
import com.hillcommerce.modules.admin.service.SalesTrendService;
import com.hillcommerce.modules.admin.service.UserProfileService;
import com.hillcommerce.modules.user.security.AuthenticatedUserPrincipal;

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
    public TrendResponse trends(
        Authentication authentication,
        @RequestParam(defaultValue = "day") String granularity,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        AuthenticatedUserPrincipal principal = requireAny(authentication);
        return salesTrendService.getTrends(granularity, from, to, principal.id(), isSales(principal));
    }

    @GetMapping("/anomalies")
    public List<AnomalyItem> anomalies(Authentication authentication) {
        requireAny(authentication);
        return anomalyDetectionService.currentAnomalies();
    }

    @GetMapping("/anomalies/status")
    public AnomalyStatusResponse anomalyStatus(Authentication authentication) {
        requireAny(authentication);
        return anomalyDetectionService.getStatus();
    }

    @PostMapping("/anomalies/{id}/acknowledge")
    public AnomalyStatusResponse acknowledge(Authentication authentication, @PathVariable String id) {
        requireAdmin(authentication);
        anomalyDetectionService.acknowledge(id);
        return anomalyDetectionService.getStatus();
    }

    @GetMapping("/rankings/products")
    public ProductRankingResponse productRankings(
        Authentication authentication,
        @RequestParam(defaultValue = "today") String range,
        @RequestParam(defaultValue = "10") int limit
    ) {
        AuthenticatedUserPrincipal principal = requireAny(authentication);
        return productRankingService.getRankings(range, limit, principal.id(), isSales(principal));
    }

    @GetMapping("/profiles/aggregate")
    public AggregateProfileResponse aggregateProfiles(Authentication authentication) {
        requireAdmin(authentication);
        return userProfileService.getAggregateProfiles();
    }

    @GetMapping("/profiles/users/search")
    public List<UserProfileSummary> searchUsers(Authentication authentication, @RequestParam String keyword) {
        requireAdmin(authentication);
        return userProfileService.searchUsers(keyword);
    }

    @GetMapping("/profiles/users/{userId}")
    public UserProfileDetail userProfile(Authentication authentication, @PathVariable Long userId) {
        requireAny(authentication);
        return userProfileService.getUserProfile(userId);
    }

    private AuthenticatedUserPrincipal requireAny(Authentication authentication) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        if (!principal.roles().contains("ADMIN") && !principal.roles().contains("SALES")) {
            throw new AccessDeniedException("forbidden");
        }
        return principal;
    }

    private void requireAdmin(Authentication authentication) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        if (!principal.roles().contains("ADMIN")) {
            throw new AccessDeniedException("forbidden");
        }
    }

    private AuthenticatedUserPrincipal principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new AccessDeniedException("forbidden");
        }
        return principal;
    }

    private boolean isSales(AuthenticatedUserPrincipal principal) {
        return principal.roles().contains("SALES") && !principal.roles().contains("ADMIN");
    }
}

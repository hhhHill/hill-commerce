package com.hillcommerce.modules.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class AdminAnalyticsDtos {

    private AdminAnalyticsDtos() {
    }

    public record TrendPoint(
        String date,
        BigDecimal amount,
        BigDecimal movingAvg,
        BigDecimal lastPeriodAmount
    ) {
    }

    public record TrendResponse(
        String granularity,
        List<TrendPoint> points,
        String trendDirection,
        BigDecimal changePercent
    ) {
    }

    public record AnomalyItem(
        String id,
        String snapshotHour,
        BigDecimal currentAmount,
        BigDecimal baselineMean,
        BigDecimal baselineStd,
        String direction,
        BigDecimal deviationPercent
    ) {
    }

    public record AnomalyStatusResponse(boolean hasAlert, int count) {
    }

    public record ProductRankItem(
        long productId,
        String productName,
        long categoryId,
        String categoryName,
        int totalQuantity,
        BigDecimal totalAmount
    ) {
    }

    public record ProductRankingResponse(String range, List<ProductRankItem> items) {
    }

    public record RegionDistribution(String region, long userCount) {
    }

    public record PurchasingPowerTier(String tier, long userCount, BigDecimal totalAmount) {
    }

    public record CategoryPreference(long categoryId, String categoryName, long orderCount) {
    }

    public record AggregateProfileResponse(
        List<RegionDistribution> regionDistribution,
        List<PurchasingPowerTier> purchasingPowerTiers,
        List<CategoryPreference> categoryPreferences,
        long totalUsers,
        long repeatPurchaseUsers,
        BigDecimal repeatPurchaseRate
    ) {
    }

    public record UserProfileSummary(long userId, String email, String nickname) {
    }

    public record UserProfileDetail(
        long userId,
        String email,
        String nickname,
        String region,
        BigDecimal totalSpent,
        String purchasingPowerTier,
        List<String> preferredCategories,
        int orderCountLast90Days
    ) {
    }

    public record AnomalyListResponse(
        List<AnomalyItem> items, int page, int size, long totalItems, boolean hasAlert
    ) {
    }

    public record TodaySnapshotResponse(
        TodayMetrics today,
        ComparisonMetrics comparison,
        List<HourlyBreakdown> hourlyBreakdown,
        List<TopProduct> topProducts
    ) {}

    public record TodayMetrics(BigDecimal revenue, int orders, BigDecimal avgOrder) {}
    public record ComparisonMetrics(BigDecimal revenueChange, BigDecimal orderChange) {}
    public record HourlyBreakdown(String hour, int orders, BigDecimal revenue) {}
    public record TopProduct(long productId, String productName, int quantity, BigDecimal revenue) {}

    public record ProductFunnelResponse(
        DateRange period,
        long totalViews,
        long totalOrders,
        BigDecimal viewToOrderRate,
        List<FunnelProduct> topByViews,
        List<FunnelProduct> topByConversion,
        List<FunnelProduct> lowConversion
    ) {}

    public record DateRange(LocalDate from, LocalDate to) {}
    public record FunnelProduct(long productId, String productName, long views, long orders,
                                BigDecimal conversionRate) {}
}

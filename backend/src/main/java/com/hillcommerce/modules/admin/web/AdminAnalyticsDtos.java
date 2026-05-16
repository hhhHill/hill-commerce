package com.hillcommerce.modules.admin.web;

import java.math.BigDecimal;
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
}

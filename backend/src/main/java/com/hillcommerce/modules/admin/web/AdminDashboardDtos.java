package com.hillcommerce.modules.admin.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class AdminDashboardDtos {

    private AdminDashboardDtos() {
    }

    public record SalesRankItem(
        String nickname,
        int orderCount
    ) {
    }

    public record DashboardSummaryResponse(
        Map<String, Long> orderStatusCounts,
        BigDecimal totalSalesAmount,
        List<SalesRankItem> salesRanking
    ) {
    }
}

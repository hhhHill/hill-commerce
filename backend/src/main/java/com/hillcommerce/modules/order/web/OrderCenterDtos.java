package com.hillcommerce.modules.order.web;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class OrderCenterDtos {

    private OrderCenterDtos() {
    }

    public record OrderListQuery(
        Integer page,
        Integer size,
        String status,
        String orderNo
    ) {
    }

    public record OrderListItemResponse(
        Long orderId,
        String orderNo,
        Long userId,
        String orderStatus,
        BigDecimal payableAmount,
        LocalDateTime createdAt,
        String summaryProductName,
        Integer summaryItemCount
    ) {
    }

    public record OrderListResponse(
        List<OrderListItemResponse> items,
        long page,
        long size,
        long total,
        long totalPages
    ) {
    }
}

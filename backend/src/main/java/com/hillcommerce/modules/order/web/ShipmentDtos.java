package com.hillcommerce.modules.order.web;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ShipmentDtos {

    private ShipmentDtos() {
    }

    public record ShipOrderRequest(
        String carrierName,
        String trackingNo
    ) {
    }

    public record ShipmentInfoResponse(
        String carrierName,
        String trackingNo,
        LocalDateTime shippedAt
    ) {
    }

    public record ShipOrderResponse(
        Long orderId,
        String orderStatus,
        Long shipmentId,
        String shipmentStatus
    ) {
    }

    public record ConfirmReceiptResponse(
        Long orderId,
        String orderStatus,
        String shipmentStatus
    ) {
    }

    public record AutoCompleteResponse(
        int completedCount
    ) {
    }

    public record AdminOrderListItemResponse(
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

    public record AdminOrderListResponse(
        List<AdminOrderListItemResponse> items,
        long page,
        long size,
        long total,
        long totalPages
    ) {
    }
}

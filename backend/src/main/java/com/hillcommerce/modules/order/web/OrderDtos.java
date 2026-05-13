package com.hillcommerce.modules.order.web;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record CheckoutAddressResponse(
        Long id,
        String receiverName,
        String receiverPhone,
        String province,
        String city,
        String district,
        String detailAddress,
        String postalCode,
        Boolean isDefault
    ) {
    }

    public record CheckoutItemResponse(
        Long id,
        Long productId,
        String productName,
        String productCoverImageUrl,
        Long skuId,
        String skuCode,
        String skuSpecText,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotalAmount,
        String anomalyCode,
        String anomalyMessage,
        Boolean canSubmit
    ) {
    }

    public record CheckoutSummaryMetaResponse(
        Integer selectedItemCount,
        Integer selectedQuantity,
        BigDecimal totalAmount,
        Integer validSelectedItemCount,
        Integer validSelectedQuantity,
        BigDecimal validTotalAmount,
        Boolean canSubmit,
        List<String> blockingReasons
    ) {
    }

    public record CheckoutResponse(
        List<CheckoutItemResponse> items,
        CheckoutAddressResponse defaultAddress,
        CheckoutSummaryMetaResponse summary
    ) {
    }

    public record CreateOrderResponse(
        Long orderId,
        String orderNo,
        String orderStatus,
        BigDecimal payableAmount
    ) {
    }

    public record OrderItemResponse(
        Long id,
        Long productId,
        Long skuId,
        String productName,
        String skuCode,
        String skuSpecText,
        String productImageUrl,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotalAmount
    ) {
    }

    public record OrderStatusHistoryResponse(
        Long id,
        String fromStatus,
        String toStatus,
        Long changedBy,
        String changeReason,
        LocalDateTime createdAt
    ) {
    }

    public record OrderDetailResponse(
        Long id,
        String orderNo,
        String orderStatus,
        BigDecimal totalAmount,
        BigDecimal payableAmount,
        LocalDateTime paymentDeadlineAt,
        CheckoutAddressResponse address,
        List<OrderItemResponse> items,
        List<OrderStatusHistoryResponse> statusHistory
    ) {
    }

    public record CancelOrderResponse(
        Long orderId,
        String orderStatus
    ) {
    }
}

package com.hillcommerce.order.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderView(
        Long orderId,
        String status,
        BigDecimal totalAmount,
        List<OrderItemView> items
) {

    public record OrderItemView(
            Long productId,
            String productName,
            BigDecimal salePrice,
            int quantity,
            BigDecimal lineAmount
    ) {
    }
}

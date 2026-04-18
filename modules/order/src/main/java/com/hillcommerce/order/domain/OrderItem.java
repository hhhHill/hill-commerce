package com.hillcommerce.order.domain;

import java.math.BigDecimal;

public record OrderItem(
        Long id,
        Long productId,
        String productName,
        BigDecimal salePrice,
        int quantity,
        BigDecimal lineAmount
) {
}

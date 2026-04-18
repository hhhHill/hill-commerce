package com.hillcommerce.cart.api.dto;

import java.math.BigDecimal;

public record CartItemView(
        Long productId,
        String productName,
        BigDecimal price,
        int quantity,
        boolean checked
) {
}

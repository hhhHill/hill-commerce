package com.hillcommerce.cart.domain;

import java.math.BigDecimal;

public record CartItem(
        Long productId,
        String productNameSnapshot,
        BigDecimal priceSnapshot,
        int quantity,
        boolean checked
) {
}

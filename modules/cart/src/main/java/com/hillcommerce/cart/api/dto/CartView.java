package com.hillcommerce.cart.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartView(Long userId, List<CartItemView> items, BigDecimal totalAmount) {
}

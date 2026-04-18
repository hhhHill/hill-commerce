package com.hillcommerce.order.domain;

import java.math.BigDecimal;
import java.util.List;

public record Order(Long id, Long userId, String status, BigDecimal totalAmount, List<OrderItem> items) {
}

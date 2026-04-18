package com.hillcommerce.product.api.dto;

import java.math.BigDecimal;

public record ProductSummary(Long id, String name, BigDecimal price, String status) {
}

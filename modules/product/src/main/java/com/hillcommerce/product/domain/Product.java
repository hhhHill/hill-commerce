package com.hillcommerce.product.domain;

import java.math.BigDecimal;

public record Product(Long id, String name, BigDecimal price, String status) {
}

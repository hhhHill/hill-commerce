package com.hillcommerce.cart.domain;

import java.util.List;

public record Cart(Long userId, List<CartItem> items) {
}

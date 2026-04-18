package com.hillcommerce.cart.api.command;

public record CheckCartItemCommand(Long productId, boolean checked) {
}

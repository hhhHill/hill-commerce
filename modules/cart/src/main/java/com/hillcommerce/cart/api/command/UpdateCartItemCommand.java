package com.hillcommerce.cart.api.command;

public record UpdateCartItemCommand(Long productId, int quantity) {
}

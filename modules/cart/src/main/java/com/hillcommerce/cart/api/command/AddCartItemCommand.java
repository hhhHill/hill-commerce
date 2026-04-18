package com.hillcommerce.cart.api.command;

public record AddCartItemCommand(Long productId, int quantity) {
}

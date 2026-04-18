package com.hillcommerce.backend.controller;

import com.hillcommerce.cart.application.CartApplicationService;
import com.hillcommerce.cart.api.command.AddCartItemCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CartControllerTest {

    @Test
    void shouldAddItemIntoCart() {
        CartController controller = new CartController(CartApplicationService.stub());

        assertEquals(1, controller.addItem(new AddCartItemCommand(1001L, 1)).data().items().size());
    }
}

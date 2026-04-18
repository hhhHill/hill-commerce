package com.hillcommerce.cart.application;

import com.hillcommerce.cart.api.command.AddCartItemCommand;
import com.hillcommerce.cart.api.command.CheckCartItemCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartApplicationServiceTest {

    @Test
    void shouldAddAndCheckCartItem() {
        CartApplicationService service = CartApplicationService.stub();

        service.addItem(1L, new AddCartItemCommand(1001L, 2));
        service.checkItem(1L, new CheckCartItemCommand(1001L, true));

        assertEquals(1, service.getCart(1L).items().size());
        assertTrue(service.getCart(1L).items().get(0).checked());
    }
}

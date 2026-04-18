package com.hillcommerce.order.application;

import com.hillcommerce.order.api.command.CreateOrderCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderApplicationServiceTest {

    @Test
    void shouldCreateOrderFromCheckedCartItems() {
        OrderApplicationService service = OrderApplicationService.stub();

        var order = service.createOrder(1L, new CreateOrderCommand());

        assertEquals("CREATED", order.status());
        assertEquals(1, order.items().size());
    }
}

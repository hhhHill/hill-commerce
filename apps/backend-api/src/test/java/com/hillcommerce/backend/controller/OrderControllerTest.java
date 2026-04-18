package com.hillcommerce.backend.controller;

import com.hillcommerce.order.api.command.CreateOrderCommand;
import com.hillcommerce.order.application.OrderApplicationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderControllerTest {

    @Test
    void shouldCreateOrder() {
        OrderController controller = new OrderController(OrderApplicationService.stub());

        assertEquals("CREATED", controller.createOrder(new CreateOrderCommand()).data().status());
    }
}

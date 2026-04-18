package com.hillcommerce.backend.controller;

import com.hillcommerce.common.core.api.ApiResponse;
import com.hillcommerce.order.api.OrderFacade;
import com.hillcommerce.order.api.command.CreateOrderCommand;
import com.hillcommerce.order.api.dto.OrderView;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private static final Long DEMO_USER_ID = 1L;

    private final OrderFacade orderFacade;

    public OrderController(OrderFacade orderFacade) {
        this.orderFacade = orderFacade;
    }

    @PostMapping
    public ApiResponse<OrderView> createOrder(@RequestBody(required = false) CreateOrderCommand command) {
        return ApiResponse.success(orderFacade.createOrder(
                DEMO_USER_ID,
                command == null ? new CreateOrderCommand() : command
        ));
    }
}

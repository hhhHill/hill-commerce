package com.hillcommerce.order.api;

import com.hillcommerce.order.api.command.CreateOrderCommand;
import com.hillcommerce.order.api.dto.OrderView;

public interface OrderFacade {

    OrderView createOrder(Long userId, CreateOrderCommand command);
}

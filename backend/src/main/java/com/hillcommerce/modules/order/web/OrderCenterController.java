package com.hillcommerce.modules.order.web;

import static com.hillcommerce.modules.order.web.OrderCenterDtos.OrderListQuery;
import static com.hillcommerce.modules.order.web.OrderCenterDtos.OrderListResponse;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hillcommerce.modules.order.service.OrderCenterService;
import com.hillcommerce.modules.user.security.AuthenticatedUserPrincipal;

@RestController
@RequestMapping("/api/orders")
public class OrderCenterController {

    private final OrderCenterService orderCenterService;

    public OrderCenterController(OrderCenterService orderCenterService) {
        this.orderCenterService = orderCenterService;
    }

    @GetMapping
    public OrderListResponse listOrders(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String orderNo,
        Authentication authentication
    ) {
        return orderCenterService.listOrders(requireUserId(authentication), new OrderListQuery(page, size, status, orderNo));
    }

    private Long requireUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new IllegalStateException("Authenticated user is required");
        }
        return principal.id();
    }
}

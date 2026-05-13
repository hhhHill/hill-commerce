package com.hillcommerce.modules.order.web;

import static com.hillcommerce.modules.order.web.OrderDtos.OrderDetailResponse;
import static com.hillcommerce.modules.order.web.OrderDtos.CancelOrderResponse;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hillcommerce.modules.order.service.OrderService;
import com.hillcommerce.modules.user.security.AuthenticatedUserPrincipal;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{orderId}")
    public OrderDetailResponse getOrder(@PathVariable Long orderId, Authentication authentication) {
        return orderService.getOrder(requireUserId(authentication), orderId);
    }

    @PostMapping("/{orderId}/cancel")
    public CancelOrderResponse cancelOrder(@PathVariable Long orderId, Authentication authentication) {
        return orderService.cancelOrder(requireUserId(authentication), orderId);
    }

    private Long requireUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new IllegalStateException("Authenticated user is required");
        }
        return principal.id();
    }
}

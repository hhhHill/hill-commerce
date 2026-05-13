package com.hillcommerce.modules.order.web;

import static com.hillcommerce.modules.order.web.OrderDtos.CheckoutResponse;
import static com.hillcommerce.modules.order.web.OrderDtos.CreateOrderResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hillcommerce.modules.order.service.OrderCheckoutService;
import com.hillcommerce.modules.user.security.AuthenticatedUserPrincipal;

@RestController
@RequestMapping("/api/orders")
public class OrderCheckoutController {

    private final OrderCheckoutService orderCheckoutService;

    public OrderCheckoutController(OrderCheckoutService orderCheckoutService) {
        this.orderCheckoutService = orderCheckoutService;
    }

    @GetMapping("/checkout")
    public CheckoutResponse getCheckout(Authentication authentication) {
        return orderCheckoutService.getCheckout(requireUserId(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateOrderResponse createOrder(Authentication authentication) {
        return orderCheckoutService.createOrder(requireUserId(authentication));
    }

    private Long requireUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new IllegalStateException("Authenticated user is required");
        }
        return principal.id();
    }
}

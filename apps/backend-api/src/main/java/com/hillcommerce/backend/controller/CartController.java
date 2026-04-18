package com.hillcommerce.backend.controller;

import com.hillcommerce.cart.api.CartFacade;
import com.hillcommerce.cart.api.command.AddCartItemCommand;
import com.hillcommerce.cart.api.command.CheckCartItemCommand;
import com.hillcommerce.cart.api.command.UpdateCartItemCommand;
import com.hillcommerce.cart.api.dto.CartView;
import com.hillcommerce.common.core.api.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private static final Long DEMO_USER_ID = 1L;

    private final CartFacade cartFacade;

    public CartController(CartFacade cartFacade) {
        this.cartFacade = cartFacade;
    }

    @PostMapping("/items")
    public ApiResponse<CartView> addItem(@RequestBody AddCartItemCommand command) {
        return ApiResponse.success(cartFacade.addItem(DEMO_USER_ID, command));
    }

    @PutMapping("/items/{productId}")
    public ApiResponse<CartView> updateItem(@PathVariable Long productId, @RequestBody UpdateQuantityRequest request) {
        return ApiResponse.success(cartFacade.updateItem(DEMO_USER_ID, new UpdateCartItemCommand(productId, request.quantity())));
    }

    @DeleteMapping("/items/{productId}")
    public ApiResponse<CartView> removeItem(@PathVariable Long productId) {
        return ApiResponse.success(cartFacade.removeItem(DEMO_USER_ID, productId));
    }

    @PutMapping("/items/{productId}/checked")
    public ApiResponse<CartView> checkItem(@PathVariable Long productId, @RequestBody CheckItemRequest request) {
        return ApiResponse.success(cartFacade.checkItem(DEMO_USER_ID, new CheckCartItemCommand(productId, request.checked())));
    }

    @GetMapping
    public ApiResponse<CartView> getCart() {
        return ApiResponse.success(cartFacade.getCart(DEMO_USER_ID));
    }

    public record UpdateQuantityRequest(int quantity) {
    }

    public record CheckItemRequest(boolean checked) {
    }
}

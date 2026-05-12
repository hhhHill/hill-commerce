package com.hillcommerce.modules.cart.web;

import static com.hillcommerce.modules.cart.web.CartDtos.AddCartItemRequest;
import static com.hillcommerce.modules.cart.web.CartDtos.CartMutationResponse;
import static com.hillcommerce.modules.cart.web.CartDtos.CartResponse;
import static com.hillcommerce.modules.cart.web.CartDtos.UpdateCartItemRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hillcommerce.modules.cart.service.CartService;
import com.hillcommerce.modules.user.security.AuthenticatedUserPrincipal;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse getCart(Authentication authentication) {
        return cartService.getCart(requireUserId(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CartMutationResponse addItem(
        Authentication authentication,
        @Valid @RequestBody AddCartItemRequest request
    ) {
        return cartService.addItem(requireUserId(authentication), request.skuId(), request.quantity());
    }

    @PutMapping("/{itemId}")
    public CartMutationResponse updateItem(
        Authentication authentication,
        @PathVariable Long itemId,
        @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return cartService.updateItem(requireUserId(authentication), itemId, request.quantity(), request.selected());
    }

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(Authentication authentication, @PathVariable Long itemId) {
        cartService.deleteItem(requireUserId(authentication), itemId);
    }

    private Long requireUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new IllegalStateException("Authenticated user is required");
        }
        return principal.id();
    }
}

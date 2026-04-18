package com.hillcommerce.cart.api;

import com.hillcommerce.cart.api.command.AddCartItemCommand;
import com.hillcommerce.cart.api.command.CheckCartItemCommand;
import com.hillcommerce.cart.api.command.UpdateCartItemCommand;
import com.hillcommerce.cart.api.dto.CartItemView;
import com.hillcommerce.cart.api.dto.CartView;

import java.util.List;

public interface CartFacade {

    CartView addItem(Long userId, AddCartItemCommand command);

    CartView updateItem(Long userId, UpdateCartItemCommand command);

    CartView removeItem(Long userId, Long productId);

    CartView checkItem(Long userId, CheckCartItemCommand command);

    CartView getCart(Long userId);

    List<CartItemView> getCheckedItems(Long userId);

    void clearCheckedItems(Long userId);
}

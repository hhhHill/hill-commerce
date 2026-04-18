package com.hillcommerce.cart.application;

import com.hillcommerce.cart.api.CartFacade;
import com.hillcommerce.cart.api.command.AddCartItemCommand;
import com.hillcommerce.cart.api.command.CheckCartItemCommand;
import com.hillcommerce.cart.api.command.UpdateCartItemCommand;
import com.hillcommerce.cart.api.dto.CartItemView;
import com.hillcommerce.cart.api.dto.CartView;
import com.hillcommerce.cart.domain.CartItem;
import com.hillcommerce.cart.infrastructure.RedisCartRepository;
import com.hillcommerce.common.core.exception.BusinessException;
import com.hillcommerce.product.api.ProductFacade;
import com.hillcommerce.product.api.dto.ProductSummary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CartApplicationService implements CartFacade {

    private final ProductFacade productFacade;
    private final RedisCartRepository redisCartRepository;
    private final Map<Long, List<CartItem>> stubStorage;

    public CartApplicationService(ProductFacade productFacade, RedisCartRepository redisCartRepository) {
        this(productFacade, redisCartRepository, null);
    }

    private CartApplicationService(ProductFacade productFacade,
                                   RedisCartRepository redisCartRepository,
                                   Map<Long, List<CartItem>> stubStorage) {
        this.productFacade = productFacade;
        this.redisCartRepository = redisCartRepository;
        this.stubStorage = stubStorage;
    }

    public static CartApplicationService stub() {
        ProductFacade stubProductFacade = new ProductFacade() {
            @Override
            public List<ProductSummary> listAvailableProducts() {
                return List.of(new ProductSummary(1001L, "Java Course", new BigDecimal("99.00"), "ON_SHELF"));
            }

            @Override
            public ProductSummary getProduct(Long productId) {
                return new ProductSummary(productId, "Java Course", new BigDecimal("99.00"), "ON_SHELF");
            }
        };

        return new CartApplicationService(stubProductFacade, null, new ConcurrentHashMap<>());
    }

    @Override
    public CartView addItem(Long userId, AddCartItemCommand command) {
        if (command.quantity() <= 0) {
            throw new BusinessException("INVALID_QUANTITY", "加入购物车数量必须大于 0");
        }

        ProductSummary product = productFacade.getProduct(command.productId());
        List<CartItem> items = new ArrayList<>(loadItems(userId));

        int existingIndex = indexOf(items, command.productId());
        if (existingIndex >= 0) {
            CartItem existing = items.get(existingIndex);
            items.set(existingIndex, new CartItem(
                    existing.productId(),
                    existing.productNameSnapshot(),
                    existing.priceSnapshot(),
                    existing.quantity() + command.quantity(),
                    existing.checked()
            ));
        } else {
            items.add(new CartItem(product.id(), product.name(), product.price(), command.quantity(), false));
        }

        saveItems(userId, items);
        return toView(userId, items);
    }

    @Override
    public CartView updateItem(Long userId, UpdateCartItemCommand command) {
        if (command.quantity() <= 0) {
            throw new BusinessException("INVALID_QUANTITY", "购物车数量必须大于 0");
        }

        List<CartItem> items = new ArrayList<>(loadItems(userId));
        int existingIndex = indexOf(items, command.productId());
        if (existingIndex < 0) {
            throw new BusinessException("CART_ITEM_NOT_FOUND", "购物车中不存在该商品");
        }

        CartItem existing = items.get(existingIndex);
        items.set(existingIndex, new CartItem(
                existing.productId(),
                existing.productNameSnapshot(),
                existing.priceSnapshot(),
                command.quantity(),
                existing.checked()
        ));

        saveItems(userId, items);
        return toView(userId, items);
    }

    @Override
    public CartView removeItem(Long userId, Long productId) {
        List<CartItem> items = new ArrayList<>(loadItems(userId));
        items.removeIf(item -> item.productId().equals(productId));
        saveItems(userId, items);
        return toView(userId, items);
    }

    @Override
    public CartView checkItem(Long userId, CheckCartItemCommand command) {
        List<CartItem> items = new ArrayList<>(loadItems(userId));
        int existingIndex = indexOf(items, command.productId());
        if (existingIndex < 0) {
            throw new BusinessException("CART_ITEM_NOT_FOUND", "购物车中不存在该商品");
        }

        CartItem existing = items.get(existingIndex);
        items.set(existingIndex, new CartItem(
                existing.productId(),
                existing.productNameSnapshot(),
                existing.priceSnapshot(),
                existing.quantity(),
                command.checked()
        ));

        saveItems(userId, items);
        return toView(userId, items);
    }

    @Override
    public CartView getCart(Long userId) {
        return toView(userId, loadItems(userId));
    }

    @Override
    public List<CartItemView> getCheckedItems(Long userId) {
        return loadItems(userId).stream()
                .filter(CartItem::checked)
                .map(this::toItemView)
                .toList();
    }

    @Override
    public void clearCheckedItems(Long userId) {
        List<CartItem> remaining = loadItems(userId).stream()
                .filter(item -> !item.checked())
                .toList();
        saveItems(userId, remaining);
    }

    private List<CartItem> loadItems(Long userId) {
        if (stubStorage != null) {
            return stubStorage.getOrDefault(userId, new ArrayList<>());
        }
        return redisCartRepository.getCartItems(userId);
    }

    private void saveItems(Long userId, List<CartItem> items) {
        if (stubStorage != null) {
            stubStorage.put(userId, new ArrayList<>(items));
            return;
        }
        redisCartRepository.saveCartItems(userId, items);
    }

    private int indexOf(List<CartItem> items, Long productId) {
        for (int index = 0; index < items.size(); index++) {
            if (items.get(index).productId().equals(productId)) {
                return index;
            }
        }
        return -1;
    }

    private CartView toView(Long userId, List<CartItem> items) {
        List<CartItemView> itemViews = items.stream().map(this::toItemView).toList();
        BigDecimal total = itemViews.stream()
                .filter(CartItemView::checked)
                .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartView(userId, itemViews, total);
    }

    private CartItemView toItemView(CartItem item) {
        return new CartItemView(
                item.productId(),
                item.productNameSnapshot(),
                item.priceSnapshot(),
                item.quantity(),
                item.checked()
        );
    }
}

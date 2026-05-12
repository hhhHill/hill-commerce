package com.hillcommerce.modules.cart.service;

import static com.hillcommerce.modules.cart.web.CartDtos.CartItemResponse;
import static com.hillcommerce.modules.cart.web.CartDtos.CartMutationResponse;
import static com.hillcommerce.modules.cart.web.CartDtos.CartResponse;
import static com.hillcommerce.modules.cart.web.CartDtos.CartSummaryResponse;
import static com.hillcommerce.modules.cart.web.CartDtos.CheckoutAddressResponse;
import static com.hillcommerce.modules.cart.web.CartDtos.CheckoutItemResponse;
import static com.hillcommerce.modules.cart.web.CartDtos.CheckoutSummaryMetaResponse;
import static com.hillcommerce.modules.cart.web.CartDtos.CheckoutSummaryResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hillcommerce.modules.cart.entity.CartEntity;
import com.hillcommerce.modules.cart.entity.CartItemEntity;
import com.hillcommerce.modules.cart.mapper.CartItemMapper;
import com.hillcommerce.modules.cart.mapper.CartMapper;
import com.hillcommerce.modules.product.entity.ProductEntity;
import com.hillcommerce.modules.product.entity.ProductSkuEntity;
import com.hillcommerce.modules.product.mapper.ProductMapper;
import com.hillcommerce.modules.product.mapper.ProductSkuMapper;
import com.hillcommerce.modules.product.service.ProductAdminService;
import com.hillcommerce.modules.user.entity.UserAddressEntity;
import com.hillcommerce.modules.user.mapper.UserAddressMapper;

@Service
public class CartService {

    private static final String PRODUCT_STATUS_ON_SHELF = ProductAdminService.PRODUCT_STATUS_ON_SHELF;
    private static final String SKU_STATUS_ENABLED = ProductAdminService.SKU_STATUS_ENABLED;
    private static final String BLOCKING_REASON_MISSING_DEFAULT_ADDRESS = "MISSING_DEFAULT_ADDRESS";
    private static final String BLOCKING_REASON_NO_SELECTED_ITEMS = "NO_SELECTED_ITEMS";
    private static final String ANOMALY_PRODUCT_OFF_SHELF = "PRODUCT_OFF_SHELF";
    private static final String ANOMALY_SKU_INVALID = "SKU_INVALID";
    private static final String ANOMALY_SKU_DISABLED = "SKU_DISABLED";
    private static final String ANOMALY_INSUFFICIENT_STOCK = "INSUFFICIENT_STOCK";

    private final CartMapper cartMapper;
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final UserAddressMapper userAddressMapper;

    public CartService(
        CartMapper cartMapper,
        CartItemMapper cartItemMapper,
        ProductMapper productMapper,
        ProductSkuMapper productSkuMapper,
        UserAddressMapper userAddressMapper
    ) {
        this.cartMapper = cartMapper;
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
        this.productSkuMapper = productSkuMapper;
        this.userAddressMapper = userAddressMapper;
    }

    public CartResponse getCart(Long userId) {
        CartEntity cart = findCart(userId);
        if (cart == null) {
            return new CartResponse(List.of(), new CartSummaryResponse(0, 0, BigDecimal.ZERO));
        }

        List<CartItemView> itemViews = loadCartItemViews(cart.getId());
        return new CartResponse(
            itemViews.stream().map(CartItemView::response).toList(),
            summarize(itemViews));
    }

    @Transactional
    public CartMutationResponse addItem(Long userId, Long skuId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }

        ProductSkuEntity sku = requirePurchasableSku(skuId);
        ProductEntity product = requirePurchasableProduct(sku.getProductId());
        validateRequestedQuantity(quantity, sku);

        CartEntity cart = findOrCreateCart(userId);
        CartItemEntity existingItem = cartItemMapper.selectOne(
            new LambdaQueryWrapper<CartItemEntity>()
                .eq(CartItemEntity::getCartId, cart.getId())
                .eq(CartItemEntity::getSkuId, skuId));

        if (existingItem == null) {
            CartItemEntity item = new CartItemEntity();
            item.setCartId(cart.getId());
            item.setProductId(product.getId());
            item.setSkuId(skuId);
            item.setQuantity(quantity);
            item.setSelected(true);
            cartItemMapper.insert(item);
            return buildMutationResponse(item.getId(), cart.getId());
        }

        int mergedQuantity = existingItem.getQuantity() + quantity;
        validateRequestedQuantity(mergedQuantity, sku);
        existingItem.setQuantity(mergedQuantity);
        existingItem.setSelected(true);
        cartItemMapper.updateById(existingItem);
        return buildMutationResponse(existingItem.getId(), cart.getId());
    }

    @Transactional
    public CartMutationResponse updateItem(Long userId, Long itemId, Integer quantity, Boolean selected) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        if (selected == null) {
            throw new IllegalArgumentException("Selected flag is required");
        }

        CartEntity cart = requireCart(userId);
        CartItemEntity item = requireOwnedItem(cart.getId(), itemId);
        ProductSkuEntity sku = requireSku(item.getSkuId());
        validateRequestedQuantity(quantity, sku);

        item.setQuantity(quantity);
        item.setSelected(selected);
        cartItemMapper.updateById(item);
        return buildMutationResponse(item.getId(), cart.getId());
    }

    @Transactional
    public void deleteItem(Long userId, Long itemId) {
        CartEntity cart = requireCart(userId);
        CartItemEntity item = requireOwnedItem(cart.getId(), itemId);
        cartItemMapper.deleteById(item.getId());
    }

    public CheckoutSummaryResponse getCheckoutSummary(Long userId) {
        CartEntity cart = findCart(userId);
        List<CartItemView> allItems = cart == null ? List.of() : loadCartItemViews(cart.getId());
        List<CartItemView> selectedItems = allItems.stream().filter(item -> item.response().selected()).toList();
        UserAddressEntity defaultAddress = findDefaultAddress(userId);

        List<CheckoutItemResponse> checkoutItems = selectedItems.stream()
            .map(this::toCheckoutItem)
            .toList();

        return new CheckoutSummaryResponse(
            checkoutItems,
            defaultAddress == null ? null : toCheckoutAddress(defaultAddress),
            summarizeCheckout(checkoutItems, defaultAddress != null));
    }

    private CartMutationResponse buildMutationResponse(Long itemId, Long cartId) {
        List<CartItemView> itemViews = loadCartItemViews(cartId);
        CartItemView updatedItem = itemViews.stream()
            .filter(item -> item.response().id().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Cart item not found after update"));
        return new CartMutationResponse(updatedItem.response(), summarize(itemViews));
    }

    private CartEntity findCart(Long userId) {
        return cartMapper.selectOne(new LambdaQueryWrapper<CartEntity>().eq(CartEntity::getUserId, userId));
    }

    private CartEntity requireCart(Long userId) {
        CartEntity cart = findCart(userId);
        if (cart == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found");
        }
        return cart;
    }

    private CartEntity findOrCreateCart(Long userId) {
        CartEntity existing = findCart(userId);
        if (existing != null) {
            return existing;
        }
        CartEntity cart = new CartEntity();
        cart.setUserId(userId);
        cartMapper.insert(cart);
        return cart;
    }

    private CartItemEntity requireOwnedItem(Long cartId, Long itemId) {
        CartItemEntity item = cartItemMapper.selectById(itemId);
        if (item == null || !item.getCartId().equals(cartId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found");
        }
        return item;
    }

    private ProductEntity requirePurchasableProduct(Long productId) {
        ProductEntity product = productMapper.selectById(productId);
        if (product == null || Boolean.TRUE.equals(product.getDeleted()) || !PRODUCT_STATUS_ON_SHELF.equals(product.getStatus())) {
            throw new IllegalArgumentException("Product is not available for cart");
        }
        return product;
    }

    private ProductSkuEntity requirePurchasableSku(Long skuId) {
        ProductSkuEntity sku = requireSku(skuId);
        if (Boolean.TRUE.equals(sku.getDeleted()) || !SKU_STATUS_ENABLED.equals(sku.getStatus())) {
            throw new IllegalArgumentException("SKU is not available for cart");
        }
        return sku;
    }

    private ProductSkuEntity requireSku(Long skuId) {
        ProductSkuEntity sku = productSkuMapper.selectById(skuId);
        if (sku == null) {
            throw new IllegalArgumentException("SKU not found");
        }
        return sku;
    }

    private void validateRequestedQuantity(int quantity, ProductSkuEntity sku) {
        int stock = sku.getStock() == null ? 0 : sku.getStock();
        if (quantity > stock) {
            throw new IllegalArgumentException("Quantity exceeds available stock");
        }
    }

    private List<CartItemView> loadCartItemViews(Long cartId) {
        List<CartItemEntity> items = cartItemMapper.selectList(
            new LambdaQueryWrapper<CartItemEntity>()
                .eq(CartItemEntity::getCartId, cartId)
                .orderByDesc(CartItemEntity::getUpdatedAt, CartItemEntity::getId));
        if (items.isEmpty()) {
            return List.of();
        }

        Map<Long, ProductEntity> products = loadProductMap(items.stream().map(CartItemEntity::getProductId).toList());
        Map<Long, ProductSkuEntity> skus = loadSkuMap(items.stream().map(CartItemEntity::getSkuId).toList());

        return items.stream()
            .map(item -> {
                ProductEntity product = products.get(item.getProductId());
                ProductSkuEntity sku = skus.get(item.getSkuId());
                if (product == null || sku == null) {
                    throw new IllegalStateException("Cart item references missing product data");
                }
                BigDecimal unitPrice = sku.getPrice();
                BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                return new CartItemView(
                    new CartItemResponse(
                        item.getId(),
                        product.getId(),
                        product.getName(),
                        product.getCoverImageUrl(),
                        sku.getId(),
                        sku.getSkuCode(),
                        sku.getSalesAttrValueText(),
                        unitPrice,
                        item.getQuantity(),
                        Boolean.TRUE.equals(item.getSelected()),
                        subtotal),
                    subtotal);
            })
            .sorted(Comparator.comparing((CartItemView view) -> view.response().id()).reversed())
            .toList();
    }

    private CartSummaryResponse summarize(List<CartItemView> itemViews) {
        int selectedItemCount = 0;
        int selectedQuantity = 0;
        BigDecimal selectedAmount = BigDecimal.ZERO;

        for (CartItemView itemView : itemViews) {
            if (!itemView.response().selected()) {
                continue;
            }
            selectedItemCount++;
            selectedQuantity += itemView.response().quantity();
            selectedAmount = selectedAmount.add(itemView.subtotalAmount());
        }

        return new CartSummaryResponse(selectedItemCount, selectedQuantity, selectedAmount);
    }

    private CheckoutItemResponse toCheckoutItem(CartItemView itemView) {
        ProductEntity product = productMapper.selectById(itemView.response().productId());
        ProductSkuEntity sku = productSkuMapper.selectById(itemView.response().skuId());

        String anomalyCode = null;
        String anomalyMessage = null;

        if (product == null || Boolean.TRUE.equals(product.getDeleted()) || !PRODUCT_STATUS_ON_SHELF.equals(product.getStatus())) {
            anomalyCode = ANOMALY_PRODUCT_OFF_SHELF;
            anomalyMessage = "商品已下架或不可售";
        }
        else if (sku == null || Boolean.TRUE.equals(sku.getDeleted())) {
            anomalyCode = ANOMALY_SKU_INVALID;
            anomalyMessage = "SKU 已失效";
        }
        else if (!SKU_STATUS_ENABLED.equals(sku.getStatus())) {
            anomalyCode = ANOMALY_SKU_DISABLED;
            anomalyMessage = "SKU 已禁用";
        }
        else {
            int stock = sku.getStock() == null ? 0 : sku.getStock();
            if (stock < itemView.response().quantity()) {
                anomalyCode = ANOMALY_INSUFFICIENT_STOCK;
                anomalyMessage = "库存不足";
            }
        }

        return new CheckoutItemResponse(
            itemView.response().id(),
            itemView.response().productId(),
            itemView.response().productName(),
            itemView.response().productCoverImageUrl(),
            itemView.response().skuId(),
            itemView.response().skuCode(),
            itemView.response().skuSpecText(),
            itemView.response().unitPrice(),
            itemView.response().quantity(),
            itemView.response().selected(),
            itemView.response().subtotalAmount(),
            anomalyCode,
            anomalyMessage,
            anomalyCode == null);
    }

    private CheckoutAddressResponse toCheckoutAddress(UserAddressEntity address) {
        return new CheckoutAddressResponse(
            address.getId(),
            address.getReceiverName(),
            address.getReceiverPhone(),
            address.getProvince(),
            address.getCity(),
            address.getDistrict(),
            address.getDetailAddress(),
            address.getPostalCode(),
            Boolean.TRUE.equals(address.getIsDefault()));
    }

    private CheckoutSummaryMetaResponse summarizeCheckout(List<CheckoutItemResponse> items, boolean hasDefaultAddress) {
        int selectedItemCount = items.size();
        int selectedQuantity = 0;
        BigDecimal selectedAmount = BigDecimal.ZERO;
        int validSelectedItemCount = 0;
        int validSelectedQuantity = 0;
        BigDecimal validSelectedAmount = BigDecimal.ZERO;
        List<String> blockingReasons = new ArrayList<>();

        for (CheckoutItemResponse item : items) {
            selectedQuantity += item.quantity();
            selectedAmount = selectedAmount.add(item.subtotalAmount());

            if (Boolean.TRUE.equals(item.canCheckout())) {
                validSelectedItemCount++;
                validSelectedQuantity += item.quantity();
                validSelectedAmount = validSelectedAmount.add(item.subtotalAmount());
            }
            else if (item.anomalyCode() != null && !blockingReasons.contains(item.anomalyCode())) {
                blockingReasons.add(item.anomalyCode());
            }
        }

        if (!hasDefaultAddress) {
            blockingReasons.addFirst(BLOCKING_REASON_MISSING_DEFAULT_ADDRESS);
        }
        if (selectedItemCount == 0) {
            blockingReasons.add(BLOCKING_REASON_NO_SELECTED_ITEMS);
        }

        boolean canProceed = hasDefaultAddress && selectedItemCount > 0 && validSelectedItemCount == selectedItemCount;

        return new CheckoutSummaryMetaResponse(
            selectedItemCount,
            selectedQuantity,
            selectedAmount,
            validSelectedItemCount,
            validSelectedQuantity,
            validSelectedAmount,
            canProceed,
            blockingReasons);
    }

    private Map<Long, ProductEntity> loadProductMap(Collection<Long> productIds) {
        return productMapper.selectBatchIds(productIds).stream()
            .collect(Collectors.toMap(ProductEntity::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, ProductSkuEntity> loadSkuMap(Collection<Long> skuIds) {
        return productSkuMapper.selectBatchIds(skuIds).stream()
            .collect(Collectors.toMap(ProductSkuEntity::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private record CartItemView(
        CartItemResponse response,
        BigDecimal subtotalAmount
    ) {
    }

    private UserAddressEntity findDefaultAddress(Long userId) {
        return userAddressMapper.selectOne(
            new LambdaQueryWrapper<UserAddressEntity>()
                .eq(UserAddressEntity::getUserId, userId)
                .eq(UserAddressEntity::getIsDefault, true));
    }
}

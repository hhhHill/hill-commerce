package com.hillcommerce.modules.cart.web;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class CartDtos {

    private CartDtos() {
    }

    public record AddCartItemRequest(
        @NotNull Long skuId,
        @NotNull @Min(1) Integer quantity
    ) {
    }

    public record UpdateCartItemRequest(
        @NotNull @Min(1) Integer quantity,
        @NotNull Boolean selected
    ) {
    }

    public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        String productCoverImageUrl,
        Long skuId,
        String skuCode,
        String skuSpecText,
        BigDecimal unitPrice,
        Integer quantity,
        Boolean selected,
        BigDecimal subtotalAmount
    ) {
    }

    public record CartSummaryResponse(
        Integer selectedItemCount,
        Integer selectedQuantity,
        BigDecimal selectedAmount
    ) {
    }

    public record CartResponse(
        List<CartItemResponse> items,
        CartSummaryResponse summary
    ) {
    }

    public record CartMutationResponse(
        CartItemResponse item,
        CartSummaryResponse summary
    ) {
    }

    public record CheckoutAddressResponse(
        Long id,
        String receiverName,
        String receiverPhone,
        String province,
        String city,
        String district,
        String detailAddress,
        String postalCode,
        Boolean isDefault
    ) {
    }

    public record CheckoutItemResponse(
        Long id,
        Long productId,
        String productName,
        String productCoverImageUrl,
        Long skuId,
        String skuCode,
        String skuSpecText,
        BigDecimal unitPrice,
        Integer quantity,
        Boolean selected,
        BigDecimal subtotalAmount,
        String anomalyCode,
        String anomalyMessage,
        Boolean canCheckout
    ) {
    }

    public record CheckoutSummaryMetaResponse(
        Integer selectedItemCount,
        Integer selectedQuantity,
        BigDecimal selectedAmount,
        Integer validSelectedItemCount,
        Integer validSelectedQuantity,
        BigDecimal validSelectedAmount,
        Boolean canProceed,
        List<String> blockingReasons
    ) {
    }

    public record CheckoutSummaryResponse(
        List<CheckoutItemResponse> items,
        CheckoutAddressResponse defaultAddress,
        CheckoutSummaryMetaResponse summary
    ) {
    }
}

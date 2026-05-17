package com.hillcommerce.modules.cart.web;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class CartDtos {

    private CartDtos() {
    }

    /** 添加到购物车请求 */
    public record AddCartItemRequest(
        @NotNull Long skuId,                         // SKU ID
        @NotNull @Min(1) Integer quantity            // 购买数量（≥1）
    ) {
    }

    /** 更新购物车项请求 */
    public record UpdateCartItemRequest(
        @NotNull @Min(1) Integer quantity,           // 新数量（≥1）
        @NotNull Boolean selected                    // 是否勾选
    ) {
    }

    /** 购物车中单个商品项 */
    public record CartItemResponse(
        Long id,                                     // 购物车项ID
        Long productId,                              // 商品（SPU）ID
        String productName,                          // 商品名称
        String productCoverImageUrl,                 // 商品封面图URL
        Long skuId,                                  // SKU ID
        String skuCode,                              // SKU编码
        String skuSpecText,                          // SKU规格文本（如"红色 / XL"）
        BigDecimal unitPrice,                        // 单价
        Integer quantity,                            // 购买数量
        Boolean selected,                            // 是否勾选
        BigDecimal subtotalAmount,                   // 小计金额（单价 × 数量）
        String anomalyCode,                          // 异常编码（如 STOCK_LOW / PRICE_CHANGE / OFFLINE 等）
        String anomalyMessage,                       // 异常提示文案
        Boolean canCheckout                          // 该项是否可结账（无异常则为 true）
    ) {
    }

    /** 购物车金额汇总 */
    public record CartSummaryResponse(
        Integer selectedItemCount,                   // 已勾选商品种类数
        Integer selectedQuantity,                    // 已勾选商品总件数
        BigDecimal selectedAmount                    // 已勾选商品总金额
    ) {
    }

    /** 购物车完整响应 */
    public record CartResponse(
        List<CartItemResponse> items,                // 购物车项列表
        CartSummaryResponse summary                  // 汇总信息
    ) {
    }

    /** 购物车增/删/改操作响应 */
    public record CartMutationResponse(
        CartItemResponse item,                       // 变更后的购物车项
        CartSummaryResponse summary                  // 更新后的汇总
    ) {
    }

    /** 结账时用户收货地址 */
    public record CheckoutAddressResponse(
        Long id,                                     // 地址ID
        String receiverName,                         // 收货人姓名
        String receiverPhone,                        // 收货人电话
        String province,                             // 省
        String city,                                 // 市
        String district,                             // 区
        String detailAddress,                        // 详细地址
        String postalCode,                           // 邮编
        Boolean isDefault                            // 是否默认地址
    ) {
    }

    /** 结账预览中的单个商品项（字段与 CartItemResponse 相同，用于结账页面展示） */
    public record CheckoutItemResponse(
        Long id,                                     // 购物车项ID
        Long productId,                              // 商品（SPU）ID
        String productName,                          // 商品名称
        String productCoverImageUrl,                 // 商品封面图URL
        Long skuId,                                  // SKU ID
        String skuCode,                              // SKU编码
        String skuSpecText,                          // SKU规格文本
        BigDecimal unitPrice,                        // 单价
        Integer quantity,                            // 购买数量
        Boolean selected,                            // 是否勾选
        BigDecimal subtotalAmount,                   // 小计金额
        String anomalyCode,                          // 异常编码
        String anomalyMessage,                       // 异常提示文案
        Boolean canCheckout                          // 该项是否可结账
    ) {
    }

    /** 结账汇总——含有效/异常商品拆分 */
    public record CheckoutSummaryMetaResponse(
        Integer selectedItemCount,                   // 已勾选商品种类数（含异常）
        Integer selectedQuantity,                    // 已勾选商品总件数（含异常）
        BigDecimal selectedAmount,                   // 已勾选商品总金额（含异常）
        Integer validSelectedItemCount,              // 无异常、可结账的商品种类数
        Integer validSelectedQuantity,               // 无异常、可结账的商品件数
        BigDecimal validSelectedAmount,              // 无异常、可结账的商品金额（实际应付金额）
        Boolean canProceed,                          // 是否可以下单（所有项无异常时为 true）
        List<String> blockingReasons                 // 阻止下单的原因列表（如"商品库存不足"）
    ) {
    }

    /** 结账预览完整响应 */
    public record CheckoutSummaryResponse(
        List<CheckoutItemResponse> items,            // 勾选的结账商品列表
        CheckoutAddressResponse defaultAddress,      // 默认收货地址
        CheckoutSummaryMetaResponse summary          // 汇总元信息
    ) {
    }
}

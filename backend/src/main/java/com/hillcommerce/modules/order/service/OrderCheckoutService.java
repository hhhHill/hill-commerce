package com.hillcommerce.modules.order.service;

import static com.hillcommerce.modules.order.web.OrderDtos.CheckoutAddressResponse;
import static com.hillcommerce.modules.order.web.OrderDtos.CheckoutItemResponse;
import static com.hillcommerce.modules.order.web.OrderDtos.CheckoutResponse;
import static com.hillcommerce.modules.order.web.OrderDtos.CheckoutSummaryMetaResponse;
import static com.hillcommerce.modules.order.web.OrderDtos.CreateOrderResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hillcommerce.modules.cart.entity.CartItemEntity;
import com.hillcommerce.modules.cart.mapper.CartItemMapper;
import com.hillcommerce.modules.cart.service.CartService;
import com.hillcommerce.modules.cart.web.CartDtos;
import com.hillcommerce.modules.common.infrastructure.BusinessIdGenerator;
import com.hillcommerce.modules.common.infrastructure.NumberPrefix;
import com.hillcommerce.modules.order.entity.OrderEntity;
import com.hillcommerce.modules.order.entity.OrderItemEntity;
import com.hillcommerce.modules.order.entity.OrderStatusHistoryEntity;
import com.hillcommerce.modules.order.mapper.OrderItemMapper;
import com.hillcommerce.modules.order.mapper.OrderMapper;
import com.hillcommerce.modules.order.mapper.OrderStatusHistoryMapper;
import com.hillcommerce.modules.product.entity.ProductSkuEntity;
import com.hillcommerce.modules.product.mapper.ProductSkuMapper;

/**
 * 订单结账服务。
 *
 * 负责从购物车到订单的完整流程：结账预览（只读校验）与订单创建（事务性写入）。
 * 创建订单时将商品快照、地址快照写入订单，保证订单数据的不可变性——
 * 后续即使商品信息或用户地址变更，已生成订单不受影响。
 */
@Service
public class OrderCheckoutService {

    private final CartService cartService;
    private final CartItemMapper cartItemMapper;
    private final ProductSkuMapper productSkuMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusHistoryMapper orderStatusHistoryMapper;
    private final BusinessIdGenerator businessIdGenerator;

    public OrderCheckoutService(
        CartService cartService,
        CartItemMapper cartItemMapper,
        ProductSkuMapper productSkuMapper,
        OrderMapper orderMapper,
        OrderItemMapper orderItemMapper,
        OrderStatusHistoryMapper orderStatusHistoryMapper,
        BusinessIdGenerator businessIdGenerator
    ) {
        this.cartService = cartService;
        this.cartItemMapper = cartItemMapper;
        this.productSkuMapper = productSkuMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderStatusHistoryMapper = orderStatusHistoryMapper;
        this.businessIdGenerator = businessIdGenerator;
    }

    /**
     * 结账预览（只读），不做任何数据变更，仅展示当前购物车选中项、默认地址和可下单状态。
     */
    public CheckoutResponse getCheckout(Long userId) {
        CartDtos.CheckoutSummaryResponse checkoutSummary = cartService.getCheckoutSummary(userId);
        return new CheckoutResponse(
            mapItems(checkoutSummary.items()),
            mapAddress(checkoutSummary.defaultAddress()),
            mapSummary(checkoutSummary.summary()));
    }

    private List<CheckoutItemResponse> mapItems(List<CartDtos.CheckoutItemResponse> items) {
        return items.stream()
            .map(item -> new CheckoutItemResponse(
                item.id(),
                item.productId(),
                item.productName(),
                item.productCoverImageUrl(),
                item.skuId(),
                item.skuCode(),
                item.skuSpecText(),
                item.unitPrice(),
                item.quantity(),
                item.subtotalAmount(),
                item.anomalyCode(),
                item.anomalyMessage(),
                item.canCheckout()))
            .toList();
    }

    private CheckoutAddressResponse mapAddress(CartDtos.CheckoutAddressResponse address) {
        if (address == null) {
            return null;
        }
        return new CheckoutAddressResponse(
            address.id(),
            address.receiverName(),
            address.receiverPhone(),
            address.province(),
            address.city(),
            address.district(),
            address.detailAddress(),
            address.postalCode(),
            address.isDefault());
    }

    private CheckoutSummaryMetaResponse mapSummary(CartDtos.CheckoutSummaryMetaResponse summary) {
        return new CheckoutSummaryMetaResponse(
            summary.selectedItemCount(),
            summary.selectedQuantity(),
            summary.selectedAmount(),
            summary.validSelectedItemCount(),
            summary.validSelectedQuantity(),
            summary.validSelectedAmount(),
            summary.canProceed(),
            summary.blockingReasons());
    }

    /**
     * 创建订单（事务性写入）。
     *
     * 流程：再次校验购物车 → 插入订单 → 扣减库存 → 写入订单明细 → 清空已购购物车项 → 记录状态历史。
     * 注意：库存扣减采用 read-modify-write，高并发下存在超卖风险，
     * 应改为 SQL 原子更新（SET stock = stock - quantity WHERE stock >= quantity）。
     */
    @Transactional
    public CreateOrderResponse createOrder(Long userId) {
        CartDtos.CheckoutSummaryResponse checkoutSummary = cartService.getCheckoutSummary(userId);
        if (!Boolean.TRUE.equals(checkoutSummary.summary().canProceed())) {
            throw new IllegalArgumentException("Selected cart items are not ready for checkout");
        }

        OrderEntity order = buildOrder(userId, checkoutSummary);
        orderMapper.insert(order);

        for (CartDtos.CheckoutItemResponse item : checkoutSummary.items()) {
            UpdateWrapper<ProductSkuEntity> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id",  item.skuId())
                    .setDecrBy(true, "stock", item.quantity());
            int updated = productSkuMapper.update(null, updateWrapper);
            if (updated == 0) {
                throw new IllegalArgumentException("Insufficient stock for SKU: " + item.skuId());
            }

            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(item.productId());
            orderItem.setSkuId(item.skuId());
            orderItem.setProductNameSnapshot(item.productName());
            orderItem.setSkuCodeSnapshot(item.skuCode());
            orderItem.setSkuAttrTextSnapshot(item.skuSpecText());
            orderItem.setProductImageSnapshot(item.productCoverImageUrl());
            orderItem.setUnitPrice(item.unitPrice());
            orderItem.setQuantity(item.quantity());
            orderItem.setSubtotalAmount(item.subtotalAmount());
            orderItemMapper.insert(orderItem);

            cartItemMapper.deleteById(item.id());
        }

        OrderStatusHistoryEntity history = new OrderStatusHistoryEntity();
        history.setOrderId(order.getId());
        history.setFromStatus(null);
        history.setToStatus(OrderStatus.PENDING_PAYMENT.name());
        history.setChangedBy(userId);
        history.setChangeReason("order created");
        orderStatusHistoryMapper.insert(history);

        return new CreateOrderResponse(order.getId(), order.getOrderNo(), order.getOrderStatus(), order.getPayableAmount());
    }

    /**
     * 构建订单实体，将地址信息快照到订单中，使订单成为独立的不可变记录。
     */
    private OrderEntity buildOrder(Long userId, CartDtos.CheckoutSummaryResponse checkoutSummary) {
        CartDtos.CheckoutAddressResponse address = checkoutSummary.defaultAddress();
        CartDtos.CheckoutSummaryMetaResponse summary = checkoutSummary.summary();

        OrderEntity order = new OrderEntity();
        order.setOrderNo(businessIdGenerator.next(NumberPrefix.ORDER));
        order.setUserId(userId);
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT.name());
        order.setTotalAmount(summary.validSelectedAmount());
        order.setPayableAmount(summary.validSelectedAmount());
        order.setPaymentDeadlineAt(LocalDateTime.now().plusMinutes(30));
        order.setAddressSnapshotName(address.receiverName());
        order.setAddressSnapshotPhone(address.receiverPhone());
        order.setAddressSnapshotProvince(address.province());
        order.setAddressSnapshotCity(address.city());
        order.setAddressSnapshotDistrict(address.district());
        order.setAddressSnapshotDetail(address.detailAddress());
        order.setAddressSnapshotPostalCode(address.postalCode());
        return order;
    }
}

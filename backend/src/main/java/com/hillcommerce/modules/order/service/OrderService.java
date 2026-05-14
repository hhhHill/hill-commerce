package com.hillcommerce.modules.order.service;

import static com.hillcommerce.modules.order.web.OrderDtos.CheckoutAddressResponse;
import static com.hillcommerce.modules.order.web.OrderDtos.CancelOrderResponse;
import static com.hillcommerce.modules.order.web.OrderDtos.OrderDetailResponse;
import static com.hillcommerce.modules.order.web.OrderDtos.OrderItemResponse;
import static com.hillcommerce.modules.order.web.OrderDtos.OrderStatusHistoryResponse;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hillcommerce.modules.order.entity.OrderEntity;
import com.hillcommerce.modules.order.entity.OrderItemEntity;
import com.hillcommerce.modules.order.entity.OrderStatusHistoryEntity;
import com.hillcommerce.modules.order.mapper.OrderItemMapper;
import com.hillcommerce.modules.order.mapper.OrderMapper;
import com.hillcommerce.modules.order.mapper.OrderStatusHistoryMapper;
import com.hillcommerce.modules.product.entity.ProductSkuEntity;
import com.hillcommerce.modules.product.mapper.ProductSkuMapper;

@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusHistoryMapper orderStatusHistoryMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ShipmentService shipmentService;

    public OrderService(
        OrderMapper orderMapper,
        OrderItemMapper orderItemMapper,
        OrderStatusHistoryMapper orderStatusHistoryMapper,
        ProductSkuMapper productSkuMapper,
        ShipmentService shipmentService
    ) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderStatusHistoryMapper = orderStatusHistoryMapper;
        this.productSkuMapper = productSkuMapper;
        this.shipmentService = shipmentService;
    }

    public OrderDetailResponse getOrder(Long userId, Long orderId) {
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }

        List<OrderItemResponse> items = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItemEntity>()
                .eq(OrderItemEntity::getOrderId, orderId)
                .orderByAsc(OrderItemEntity::getId))
            .stream()
            .map(item -> new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getSkuId(),
                item.getProductNameSnapshot(),
                item.getSkuCodeSnapshot(),
                item.getSkuAttrTextSnapshot(),
                item.getProductImageSnapshot(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotalAmount()))
            .toList();

        List<OrderStatusHistoryResponse> statusHistory = orderStatusHistoryMapper.selectList(
            new LambdaQueryWrapper<OrderStatusHistoryEntity>()
                .eq(OrderStatusHistoryEntity::getOrderId, orderId)
                .orderByAsc(OrderStatusHistoryEntity::getId))
            .stream()
            .map(history -> new OrderStatusHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getChangedBy(),
                history.getChangeReason(),
                history.getCreatedAt()))
            .toList();

        return new OrderDetailResponse(
            order.getId(),
            order.getOrderNo(),
            order.getOrderStatus(),
            order.getTotalAmount(),
            order.getPayableAmount(),
            order.getPaymentDeadlineAt(),
            new CheckoutAddressResponse(
                null,
                order.getAddressSnapshotName(),
                order.getAddressSnapshotPhone(),
                order.getAddressSnapshotProvince(),
                order.getAddressSnapshotCity(),
                order.getAddressSnapshotDistrict(),
                order.getAddressSnapshotDetail(),
                order.getAddressSnapshotPostalCode(),
                true),
            items,
            statusHistory,
            shipmentService.getShipmentInfo(orderId));
    }

    @Transactional
    public CancelOrderResponse cancelOrder(Long userId, Long orderId) {
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }

        if (OrderStatus.CANCELLED.name().equals(order.getOrderStatus())) {
            return new CancelOrderResponse(order.getId(), order.getOrderStatus());
        }
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("Only pending payment orders can be cancelled");
        }

        List<OrderItemEntity> items = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItemEntity>()
                .eq(OrderItemEntity::getOrderId, orderId)
                .orderByAsc(OrderItemEntity::getId));

        for (OrderItemEntity item : items) {
            ProductSkuEntity sku = productSkuMapper.selectById(item.getSkuId());
            if (sku != null) {
                int currentStock = sku.getStock() == null ? 0 : sku.getStock();
                sku.setStock(currentStock + item.getQuantity());
                productSkuMapper.updateById(sku);
            }
        }

        order.setOrderStatus(OrderStatus.CANCELLED.name());
        orderMapper.updateById(order);

        OrderStatusHistoryEntity history = new OrderStatusHistoryEntity();
        history.setOrderId(order.getId());
        history.setFromStatus(OrderStatus.PENDING_PAYMENT.name());
        history.setToStatus(OrderStatus.CANCELLED.name());
        history.setChangedBy(userId);
        history.setChangeReason("order cancelled");
        orderStatusHistoryMapper.insert(history);

        return new CancelOrderResponse(order.getId(), order.getOrderStatus());
    }
}

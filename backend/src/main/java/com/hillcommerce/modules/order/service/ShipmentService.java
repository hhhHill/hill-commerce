package com.hillcommerce.modules.order.service;

import static com.hillcommerce.modules.order.web.OrderCenterDtos.OrderListQuery;
import static com.hillcommerce.modules.order.web.OrderDtos.CheckoutAddressResponse;
import static com.hillcommerce.modules.order.web.OrderDtos.OrderDetailResponse;
import static com.hillcommerce.modules.order.web.OrderDtos.OrderItemResponse;
import static com.hillcommerce.modules.order.web.OrderDtos.OrderStatusHistoryResponse;
import static com.hillcommerce.modules.order.web.ShipmentDtos.AdminOrderListItemResponse;
import static com.hillcommerce.modules.order.web.ShipmentDtos.AdminOrderListResponse;
import static com.hillcommerce.modules.order.web.ShipmentDtos.AutoCompleteResponse;
import static com.hillcommerce.modules.order.web.ShipmentDtos.ConfirmReceiptResponse;
import static com.hillcommerce.modules.order.web.ShipmentDtos.ShipOrderResponse;
import static com.hillcommerce.modules.order.web.ShipmentDtos.ShipmentInfoResponse;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hillcommerce.modules.order.entity.OrderEntity;
import com.hillcommerce.modules.order.entity.OrderItemEntity;
import com.hillcommerce.modules.order.entity.OrderStatusHistoryEntity;
import com.hillcommerce.modules.order.entity.ShipmentEntity;
import com.hillcommerce.modules.order.mapper.OrderItemMapper;
import com.hillcommerce.modules.order.mapper.OrderMapper;
import com.hillcommerce.modules.order.mapper.OrderStatusHistoryMapper;
import com.hillcommerce.modules.order.mapper.ShipmentMapper;

@Service
public class ShipmentService {

    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final String SHIP_REASON = "order shipped";
    private static final String USER_RECEIVED_REASON = "user confirmed receipt";
    private static final String AUTO_COMPLETED_REASON = "system auto completed";

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusHistoryMapper orderStatusHistoryMapper;
    private final ShipmentMapper shipmentMapper;
    private final OrderCenterService orderCenterService;

    public ShipmentService(
        OrderMapper orderMapper,
        OrderItemMapper orderItemMapper,
        OrderStatusHistoryMapper orderStatusHistoryMapper,
        ShipmentMapper shipmentMapper,
        OrderCenterService orderCenterService
    ) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderStatusHistoryMapper = orderStatusHistoryMapper;
        this.shipmentMapper = shipmentMapper;
        this.orderCenterService = orderCenterService;
    }

    public OrderDetailResponse getShipOrderDetail(Long orderId) {
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        if (!OrderStatus.PAID.name().equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("Only paid orders can be shipped");
        }
        return toOrderDetailResponse(order, null);
    }

    public ShipmentInfoResponse getShipmentInfo(Long orderId) {
        ShipmentEntity shipment = findLatestShipment(orderId);
        if (shipment == null) {
            return null;
        }
        OrderEntity order = orderMapper.selectById(orderId);
        LocalDateTime shippedAt = order == null ? null : order.getShippedAt();
        return new ShipmentInfoResponse(shipment.getCarrierName(), shipment.getTrackingNo(), shippedAt);
    }

    public AdminOrderListResponse listAllOrders(OrderListQuery query) {
        var result = orderCenterService.listAllOrders(query);
        List<AdminOrderListItemResponse> items = result.items().stream()
            .map(item -> new AdminOrderListItemResponse(
                item.orderId(),
                item.orderNo(),
                item.userId(),
                item.orderStatus(),
                item.payableAmount(),
                item.createdAt(),
                item.summaryProductName(),
                item.summaryItemCount()))
            .toList();
        return new AdminOrderListResponse(items, result.page(), result.size(), result.total(), result.totalPages());
    }

    @Transactional
    public ShipOrderResponse shipOrder(Long operatorUserId, Long orderId, String carrierName, String trackingNo) {
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        if (!OrderStatus.PAID.name().equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("Only paid orders can be shipped");
        }

        String normalizedCarrierName = requireText(carrierName, "Carrier name is required");
        String normalizedTrackingNo = requireText(trackingNo, "Tracking number is required");
        ShipmentEntity existingShipment = findActiveShipment(orderId);
        if (existingShipment != null) {
            throw new IllegalArgumentException("Order has already been shipped");
        }

        LocalDateTime now = LocalDateTime.now();
        OrderEntity patch = new OrderEntity();
        patch.setOrderStatus(OrderStatus.SHIPPED.name());
        patch.setShippedAt(now);

        int updated = orderMapper.update(
            patch,
            new LambdaUpdateWrapper<OrderEntity>()
                .eq(OrderEntity::getId, orderId)
                .eq(OrderEntity::getOrderStatus, OrderStatus.PAID.name()));
        if (updated == 0) {
            throw new IllegalArgumentException("Only paid orders can be shipped");
        }

        ShipmentEntity shipment = new ShipmentEntity();
        shipment.setOrderId(orderId);
        shipment.setCarrierName(normalizedCarrierName);
        shipment.setTrackingNo(normalizedTrackingNo);
        shipment.setShipmentStatus(ShipmentStatus.SHIPPED.name());
        shipment.setOperatedBy(operatorUserId);
        shipmentMapper.insert(shipment);

        insertOrderHistory(orderId, OrderStatus.PAID.name(), OrderStatus.SHIPPED.name(), operatorUserId, SHIP_REASON);
        return new ShipOrderResponse(orderId, OrderStatus.SHIPPED.name(), shipment.getId(), ShipmentStatus.SHIPPED.name());
    }

    @Transactional
    public ConfirmReceiptResponse confirmReceipt(Long userId, Long orderId) {
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }

        ShipmentEntity shipment = findLatestShipment(orderId);
        if (OrderStatus.COMPLETED.name().equals(order.getOrderStatus())) {
            return new ConfirmReceiptResponse(
                orderId,
                OrderStatus.COMPLETED.name(),
                shipment == null ? ShipmentStatus.DELIVERED.name() : shipment.getShipmentStatus());
        }
        if (!OrderStatus.SHIPPED.name().equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("Only shipped orders can be confirmed");
        }
        if (shipment == null || !ShipmentStatus.SHIPPED.name().equals(shipment.getShipmentStatus())) {
            throw new IllegalArgumentException("Shipment is not ready for receipt confirmation");
        }

        boolean completed = completeOrder(order, shipment, userId, USER_RECEIVED_REASON);
        if (!completed) {
            OrderEntity latestOrder = orderMapper.selectById(orderId);
            ShipmentEntity latestShipment = findLatestShipment(orderId);
            return new ConfirmReceiptResponse(
                orderId,
                latestOrder == null ? OrderStatus.COMPLETED.name() : latestOrder.getOrderStatus(),
                latestShipment == null ? ShipmentStatus.DELIVERED.name() : latestShipment.getShipmentStatus());
        }
        return new ConfirmReceiptResponse(orderId, OrderStatus.COMPLETED.name(), ShipmentStatus.DELIVERED.name());
    }

    @Scheduled(fixedDelayString = "${hill.fulfillment.auto-complete.fixed-delay-ms:300000}")
    public void scheduledAutoComplete() {
        autoComplete();
    }

    @Transactional
    public AutoCompleteResponse autoComplete() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(10);
        List<OrderEntity> candidates = orderMapper.selectList(
            new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getOrderStatus, OrderStatus.SHIPPED.name())
                .isNotNull(OrderEntity::getShippedAt)
                .lt(OrderEntity::getShippedAt, threshold)
                .orderByAsc(OrderEntity::getId)
                .last("limit " + DEFAULT_BATCH_SIZE));

        int completedCount = 0;
        for (OrderEntity candidate : candidates) {
            ShipmentEntity shipment = findLatestShipment(candidate.getId());
            if (shipment == null || !ShipmentStatus.SHIPPED.name().equals(shipment.getShipmentStatus())) {
                continue;
            }
            completedCount += completeOrder(candidate, shipment, null, AUTO_COMPLETED_REASON) ? 1 : 0;
        }
        return new AutoCompleteResponse(completedCount);
    }

    private boolean completeOrder(OrderEntity order, ShipmentEntity shipment, Long changedBy, String reason) {
        LocalDateTime now = LocalDateTime.now();
        OrderEntity orderPatch = new OrderEntity();
        orderPatch.setOrderStatus(OrderStatus.COMPLETED.name());
        orderPatch.setCompletedAt(now);

        int orderUpdated = orderMapper.update(
            orderPatch,
            new LambdaUpdateWrapper<OrderEntity>()
                .eq(OrderEntity::getId, order.getId())
                .eq(OrderEntity::getOrderStatus, OrderStatus.SHIPPED.name()));
        if (orderUpdated == 0) {
            return false;
        }

        ShipmentEntity shipmentPatch = new ShipmentEntity();
        shipmentPatch.setShipmentStatus(ShipmentStatus.DELIVERED.name());
        shipmentMapper.update(
            shipmentPatch,
            new LambdaUpdateWrapper<ShipmentEntity>()
                .eq(ShipmentEntity::getId, shipment.getId())
                .eq(ShipmentEntity::getShipmentStatus, ShipmentStatus.SHIPPED.name()));

        insertOrderHistory(order.getId(), OrderStatus.SHIPPED.name(), OrderStatus.COMPLETED.name(), changedBy, reason);
        return true;
    }

    private OrderDetailResponse toOrderDetailResponse(OrderEntity order, ShipmentInfoResponse shipment) {
        List<OrderItemResponse> items = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItemEntity>()
                .eq(OrderItemEntity::getOrderId, order.getId())
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
                .eq(OrderStatusHistoryEntity::getOrderId, order.getId())
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
            shipment);
    }

    private ShipmentEntity findActiveShipment(Long orderId) {
        return shipmentMapper.selectOne(
            new LambdaQueryWrapper<ShipmentEntity>()
                .eq(ShipmentEntity::getOrderId, orderId)
                .in(ShipmentEntity::getShipmentStatus, ShipmentStatus.SHIPPED.name(), ShipmentStatus.DELIVERED.name())
                .orderByDesc(ShipmentEntity::getId)
                .last("limit 1"));
    }

    private ShipmentEntity findLatestShipment(Long orderId) {
        return shipmentMapper.selectOne(
            new LambdaQueryWrapper<ShipmentEntity>()
                .eq(ShipmentEntity::getOrderId, orderId)
                .orderByDesc(ShipmentEntity::getId)
                .last("limit 1"));
    }

    private void insertOrderHistory(Long orderId, String fromStatus, String toStatus, Long changedBy, String reason) {
        OrderStatusHistoryEntity history = new OrderStatusHistoryEntity();
        history.setOrderId(orderId);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setChangedBy(changedBy);
        history.setChangeReason(reason);
        orderStatusHistoryMapper.insert(history);
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}

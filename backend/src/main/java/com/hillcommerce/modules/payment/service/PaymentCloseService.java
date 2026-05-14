package com.hillcommerce.modules.payment.service;

import static com.hillcommerce.modules.payment.web.PaymentDtos.CloseExpiredPaymentsResponse;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hillcommerce.modules.order.entity.OrderEntity;
import com.hillcommerce.modules.order.entity.OrderItemEntity;
import com.hillcommerce.modules.order.entity.OrderStatusHistoryEntity;
import com.hillcommerce.modules.order.mapper.OrderItemMapper;
import com.hillcommerce.modules.order.mapper.OrderMapper;
import com.hillcommerce.modules.order.mapper.OrderStatusHistoryMapper;
import com.hillcommerce.modules.order.service.OrderStatus;
import com.hillcommerce.modules.payment.entity.PaymentEntity;
import com.hillcommerce.modules.payment.mapper.PaymentMapper;
import com.hillcommerce.modules.product.entity.ProductSkuEntity;
import com.hillcommerce.modules.product.mapper.ProductSkuMapper;

@Service
public class PaymentCloseService {

    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final String CLOSE_REASON = "payment timeout";

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusHistoryMapper orderStatusHistoryMapper;
    private final PaymentMapper paymentMapper;
    private final ProductSkuMapper productSkuMapper;

    public PaymentCloseService(
        OrderMapper orderMapper,
        OrderItemMapper orderItemMapper,
        OrderStatusHistoryMapper orderStatusHistoryMapper,
        PaymentMapper paymentMapper,
        ProductSkuMapper productSkuMapper
    ) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderStatusHistoryMapper = orderStatusHistoryMapper;
        this.paymentMapper = paymentMapper;
        this.productSkuMapper = productSkuMapper;
    }

    @Scheduled(fixedDelayString = "${hill.payment.close-expired.fixed-delay-ms:60000}")
    public void scheduledCloseExpiredPayments() {
        closeExpiredPayments();
    }

    @Transactional
    public CloseExpiredPaymentsResponse closeExpiredPayments() {
        LocalDateTime now = LocalDateTime.now();
        List<OrderEntity> candidates = orderMapper.selectList(
            new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getOrderStatus, OrderStatus.PENDING_PAYMENT.name())
                .isNotNull(OrderEntity::getPaymentDeadlineAt)
                .lt(OrderEntity::getPaymentDeadlineAt, now)
                .orderByAsc(OrderEntity::getId)
                .last("limit " + DEFAULT_BATCH_SIZE));

        int closedOrderCount = 0;
        for (OrderEntity order : candidates) {
            closedOrderCount += closeExpiredOrder(order, now) ? 1 : 0;
        }

        return new CloseExpiredPaymentsResponse(closedOrderCount);
    }

    private boolean closeExpiredOrder(OrderEntity order, LocalDateTime now) {
        OrderEntity patch = new OrderEntity();
        patch.setOrderStatus(OrderStatus.CLOSED.name());
        patch.setCancelledAt(now);
        patch.setCancelReason(CLOSE_REASON);

        int updated = orderMapper.update(
            patch,
            new LambdaUpdateWrapper<OrderEntity>()
                .eq(OrderEntity::getId, order.getId())
                .eq(OrderEntity::getOrderStatus, OrderStatus.PENDING_PAYMENT.name())
                .lt(OrderEntity::getPaymentDeadlineAt, now));
        if (updated == 0) {
            return false;
        }

        List<OrderItemEntity> items = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItemEntity>()
                .eq(OrderItemEntity::getOrderId, order.getId())
                .orderByAsc(OrderItemEntity::getId));
        for (OrderItemEntity item : items) {
            ProductSkuEntity sku = productSkuMapper.selectById(item.getSkuId());
            if (sku != null) {
                int currentStock = sku.getStock() == null ? 0 : sku.getStock();
                sku.setStock(currentStock + item.getQuantity());
                productSkuMapper.updateById(sku);
            }
        }

        PaymentEntity paymentPatch = new PaymentEntity();
        paymentPatch.setPaymentStatus(PaymentStatus.CLOSED.name());
        paymentPatch.setClosedAt(now);
        paymentPatch.setFailureReason(CLOSE_REASON);
        paymentMapper.update(
            paymentPatch,
            new LambdaUpdateWrapper<PaymentEntity>()
                .eq(PaymentEntity::getOrderId, order.getId())
                .eq(PaymentEntity::getPaymentStatus, PaymentStatus.INITIATED.name()));

        OrderStatusHistoryEntity history = new OrderStatusHistoryEntity();
        history.setOrderId(order.getId());
        history.setFromStatus(OrderStatus.PENDING_PAYMENT.name());
        history.setToStatus(OrderStatus.CLOSED.name());
        history.setChangedBy(order.getUserId());
        history.setChangeReason(CLOSE_REASON);
        orderStatusHistoryMapper.insert(history);

        return true;
    }
}

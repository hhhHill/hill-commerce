package com.hillcommerce.modules.payment.service;

import static com.hillcommerce.modules.payment.web.PaymentDtos.PaymentAttemptResponse;
import static com.hillcommerce.modules.payment.web.PaymentDtos.PaymentActionResponse;
import static com.hillcommerce.modules.payment.web.PaymentDtos.PaymentOrderResponse;

import java.time.LocalDateTime;
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
import com.hillcommerce.modules.order.service.OrderStatus;
import com.hillcommerce.modules.payment.entity.PaymentEntity;
import com.hillcommerce.modules.payment.mapper.PaymentMapper;
import com.hillcommerce.modules.recommendation.GorseFeedbackService;

@Service
public class PaymentService {

    private static final String SIMULATED_METHOD = "SIMULATED";

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusHistoryMapper orderStatusHistoryMapper;
    private final PaymentMapper paymentMapper;
    private final PaymentNumberGenerator paymentNumberGenerator;
    private final GorseFeedbackService gorseFeedbackService;

    public PaymentService(
        OrderMapper orderMapper,
        OrderItemMapper orderItemMapper,
        OrderStatusHistoryMapper orderStatusHistoryMapper,
        PaymentMapper paymentMapper,
        PaymentNumberGenerator paymentNumberGenerator,
        GorseFeedbackService gorseFeedbackService
    ) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderStatusHistoryMapper = orderStatusHistoryMapper;
        this.paymentMapper = paymentMapper;
        this.paymentNumberGenerator = paymentNumberGenerator;
        this.gorseFeedbackService = gorseFeedbackService;
    }

    public PaymentOrderResponse getPaymentOrder(Long userId, Long orderId) {
        OrderEntity order = requireOwnedOrder(userId, orderId);
        List<PaymentAttemptResponse> attempts = loadAttempts(orderId);
        PaymentAttemptResponse currentAttempt = resolveCurrentAttempt(attempts);

        return new PaymentOrderResponse(
            order.getId(),
            order.getOrderNo(),
            order.getOrderStatus(),
            order.getPayableAmount(),
            order.getPaymentDeadlineAt(),
            currentAttempt,
            attempts);
    }

    @Transactional
    public PaymentAttemptResponse createOrReuseAttempt(Long userId, Long orderId) {
        OrderEntity order = requireOwnedOrder(userId, orderId);
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("Only pending payment orders can create payment attempts");
        }

        PaymentEntity existingAttempt = paymentMapper.selectOne(
            new LambdaQueryWrapper<PaymentEntity>()
                .eq(PaymentEntity::getOrderId, orderId)
                .eq(PaymentEntity::getPaymentStatus, PaymentStatus.INITIATED.name())
                .orderByDesc(PaymentEntity::getId)
                .last("limit 1"));
        if (existingAttempt != null) {
            return toAttemptResponse(existingAttempt);
        }

        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(order.getId());
        payment.setUserId(userId);
        payment.setPaymentNo(paymentNumberGenerator.nextPaymentNo());
        payment.setPaymentMethod(SIMULATED_METHOD);
        payment.setPaymentStatus(PaymentStatus.INITIATED.name());
        payment.setAmount(order.getPayableAmount());
        payment.setRequestedAt(LocalDateTime.now());
        paymentMapper.insert(payment);

        return toAttemptResponse(payment);
    }

    @Transactional
    public PaymentActionResponse succeed(Long userId, Long paymentId) {
        PaymentEntity payment = requireOwnedPayment(userId, paymentId);
        OrderEntity order = requireOwnedOrder(userId, payment.getOrderId());

        if (PaymentStatus.SUCCESS.name().equals(payment.getPaymentStatus())) {
            return toActionResponse(payment, order);
        }
        if (!PaymentStatus.INITIATED.name().equals(payment.getPaymentStatus())) {
            throw new IllegalArgumentException("Only initiated payment attempts can succeed");
        }
        if (OrderStatus.PAID.name().equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("Order is already paid");
        }
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("Only pending payment orders can be paid");
        }

        LocalDateTime paidAt = LocalDateTime.now();
        payment.setPaymentStatus(PaymentStatus.SUCCESS.name());
        payment.setPaidAt(paidAt);
        payment.setFailureReason(null);
        paymentMapper.updateById(payment);

        order.setOrderStatus(OrderStatus.PAID.name());
        order.setPaidAt(paidAt);
        orderMapper.updateById(order);

        OrderStatusHistoryEntity history = new OrderStatusHistoryEntity();
        history.setOrderId(order.getId());
        history.setFromStatus(OrderStatus.PENDING_PAYMENT.name());
        history.setToStatus(OrderStatus.PAID.name());
        history.setChangedBy(userId);
        history.setChangeReason("payment succeeded");
        orderStatusHistoryMapper.insert(history);
        sendPurchaseFeedback(userId, order.getId());

        return toActionResponse(payment, order);
    }

    @Transactional
    public PaymentActionResponse fail(Long userId, Long paymentId) {
        PaymentEntity payment = requireOwnedPayment(userId, paymentId);
        OrderEntity order = requireOwnedOrder(userId, payment.getOrderId());

        if (PaymentStatus.FAILED.name().equals(payment.getPaymentStatus())) {
            return toActionResponse(payment, order);
        }
        if (PaymentStatus.SUCCESS.name().equals(payment.getPaymentStatus())) {
            throw new IllegalArgumentException("Successful payment cannot be marked as failed");
        }
        if (!PaymentStatus.INITIATED.name().equals(payment.getPaymentStatus())) {
            throw new IllegalArgumentException("Only initiated payment attempts can fail");
        }
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("Only pending payment orders can fail");
        }

        payment.setPaymentStatus(PaymentStatus.FAILED.name());
        payment.setFailureReason("simulated payment failed");
        paymentMapper.updateById(payment);

        return toActionResponse(payment, order);
    }

    private OrderEntity requireOwnedOrder(Long userId, Long orderId) {
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        return order;
    }

    private PaymentEntity requireOwnedPayment(Long userId, Long paymentId) {
        PaymentEntity payment = paymentMapper.selectById(paymentId);
        if (payment == null || payment.getUserId() == null || !payment.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
        }
        return payment;
    }

    private List<PaymentAttemptResponse> loadAttempts(Long orderId) {
        return paymentMapper.selectList(
            new LambdaQueryWrapper<PaymentEntity>()
                .eq(PaymentEntity::getOrderId, orderId)
                .orderByDesc(PaymentEntity::getId))
            .stream()
            .map(this::toAttemptResponse)
            .toList();
    }

    private void sendPurchaseFeedback(Long userId, Long orderId) {
        List<OrderItemEntity> items = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItemEntity>().eq(OrderItemEntity::getOrderId, orderId));
        for (OrderItemEntity item : items) {
            gorseFeedbackService.fireAndForgetPurchase(userId, item.getProductId());
        }
    }

    private PaymentAttemptResponse resolveCurrentAttempt(List<PaymentAttemptResponse> attempts) {
        return attempts.stream()
            .filter(attempt -> PaymentStatus.INITIATED.name().equals(attempt.paymentStatus()))
            .findFirst()
            .orElseGet(() -> attempts.isEmpty() ? null : attempts.get(0));
    }

    private PaymentAttemptResponse toAttemptResponse(PaymentEntity payment) {
        return new PaymentAttemptResponse(
            payment.getId(),
            payment.getPaymentNo(),
            payment.getPaymentMethod(),
            payment.getPaymentStatus(),
            payment.getAmount(),
            payment.getRequestedAt(),
            payment.getPaidAt(),
            payment.getClosedAt(),
            payment.getFailureReason());
    }

    private PaymentActionResponse toActionResponse(PaymentEntity payment, OrderEntity order) {
        return new PaymentActionResponse(
            payment.getId(),
            payment.getPaymentStatus(),
            order.getId(),
            order.getOrderStatus(),
            payment.getPaidAt(),
            payment.getFailureReason());
    }
}

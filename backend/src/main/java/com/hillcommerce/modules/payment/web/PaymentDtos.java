package com.hillcommerce.modules.payment.web;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class PaymentDtos {

    private PaymentDtos() {
    }

    public record PaymentAttemptResponse(
        Long paymentId,
        String paymentNo,
        String paymentMethod,
        String paymentStatus,
        BigDecimal amount,
        LocalDateTime requestedAt,
        LocalDateTime paidAt,
        LocalDateTime closedAt,
        String failureReason
    ) {
    }

    public record PaymentOrderResponse(
        Long orderId,
        String orderNo,
        String orderStatus,
        BigDecimal payableAmount,
        LocalDateTime paymentDeadlineAt,
        PaymentAttemptResponse currentAttempt,
        List<PaymentAttemptResponse> attempts
    ) {
    }

    public record PaymentActionResponse(
        Long paymentId,
        String paymentStatus,
        Long orderId,
        String orderStatus,
        LocalDateTime paidAt,
        String failureReason
    ) {
    }

    public record CloseExpiredPaymentsResponse(
        int closedOrderCount
    ) {
    }
}

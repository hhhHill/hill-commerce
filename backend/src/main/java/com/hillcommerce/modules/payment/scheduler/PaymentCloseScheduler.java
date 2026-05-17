package com.hillcommerce.modules.payment.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.hillcommerce.modules.payment.service.PaymentCloseService;

@Service
public class PaymentCloseScheduler {

    private final PaymentCloseService paymentCloseService;

    public PaymentCloseScheduler(PaymentCloseService paymentCloseService) {
        this.paymentCloseService = paymentCloseService;
    }

    @Scheduled(fixedDelayString = "${hill.payment.close-expired.fixed-delay-ms:60000}")
    public void scheduledCloseExpiredPayments() {
        paymentCloseService.closeExpiredPayments();
    }
}

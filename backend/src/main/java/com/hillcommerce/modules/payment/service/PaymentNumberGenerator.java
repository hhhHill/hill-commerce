package com.hillcommerce.modules.payment.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

@Component
public class PaymentNumberGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    public String nextPaymentNo() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int suffix = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "PAY" + timestamp + suffix;
    }
}

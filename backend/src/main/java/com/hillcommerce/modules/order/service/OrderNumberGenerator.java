package com.hillcommerce.modules.order.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

@Component
public class OrderNumberGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    public String nextOrderNo() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int suffix = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "ORD" + timestamp + suffix;
    }
}

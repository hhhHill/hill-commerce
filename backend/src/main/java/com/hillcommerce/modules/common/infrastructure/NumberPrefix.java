package com.hillcommerce.modules.common.infrastructure;

public enum NumberPrefix {
    ORDER("ORD"),
    PAYMENT("PAY");

    private final String code;

    NumberPrefix(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

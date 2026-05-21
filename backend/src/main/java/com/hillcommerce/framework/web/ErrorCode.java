package com.hillcommerce.framework.web;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // ── 1xxx 通用 ────────────────────────────────────────────
    BAD_REQUEST(400, HttpStatus.BAD_REQUEST),
    NOT_FOUND(404, HttpStatus.NOT_FOUND),
    INTERNAL_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR),

    // ── 2xxx 用户与认证 ──────────────────────────────────────
    USER_NOT_FOUND(2001, HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_EXISTS(2002, HttpStatus.BAD_REQUEST),
    AUTHENTICATION_REQUIRED(2003, HttpStatus.UNAUTHORIZED),
    ROLE_NOT_FOUND(2004, HttpStatus.INTERNAL_SERVER_ERROR),
    ADDRESS_NOT_FOUND(2005, HttpStatus.NOT_FOUND),
    MERCHANT_USER_NOT_FOUND(2006, HttpStatus.NOT_FOUND),
    CANNOT_DISABLE_SELF(2007, HttpStatus.BAD_REQUEST),

    // ── 3xxx 商品 ────────────────────────────────────────────
    PRODUCT_NOT_FOUND(3001, HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND(3002, HttpStatus.NOT_FOUND),
    CATEGORY_NAME_DUPLICATE(3003, HttpStatus.BAD_REQUEST),
    CATEGORY_IN_USE(3004, HttpStatus.BAD_REQUEST),
    CATEGORY_MUST_BE_ENABLED(3005, HttpStatus.BAD_REQUEST),
    SPU_CODE_DUPLICATE(3006, HttpStatus.BAD_REQUEST),
    SKU_CODE_DUPLICATE(3007, HttpStatus.BAD_REQUEST),
    SKU_NOT_FOUND(3008, HttpStatus.NOT_FOUND),
    SKU_NOT_AVAILABLE(3009, HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_AVAILABLE(3010, HttpStatus.BAD_REQUEST),
    AT_LEAST_ONE_SKU_REQUIRED(3011, HttpStatus.BAD_REQUEST),
    MAX_SALES_ATTRIBUTES_EXCEEDED(3012, HttpStatus.BAD_REQUEST),
    SALES_ATTR_VALUES_NOT_UNIQUE(3013, HttpStatus.BAD_REQUEST),
    SKU_COMBINATIONS_NOT_UNIQUE(3014, HttpStatus.BAD_REQUEST),
    SKU_CODES_NOT_UNIQUE(3015, HttpStatus.BAD_REQUEST),
    UNSUPPORTED_CATEGORY_STATUS(3016, HttpStatus.BAD_REQUEST),
    UNSUPPORTED_PRODUCT_STATUS(3017, HttpStatus.BAD_REQUEST),
    UNSUPPORTED_SKU_STATUS(3018, HttpStatus.BAD_REQUEST),

    // ── 4xxx 订单 ────────────────────────────────────────────
    ORDER_NOT_FOUND(4001, HttpStatus.NOT_FOUND),
    ORDER_NOT_PENDING_PAYMENT(4002, HttpStatus.BAD_REQUEST),
    ONLY_PAID_ORDERS_CAN_BE_SHIPPED(4003, HttpStatus.BAD_REQUEST),
    ORDER_ALREADY_SHIPPED(4004, HttpStatus.BAD_REQUEST),
    ONLY_SHIPPED_ORDERS_CAN_BE_CONFIRMED(4005, HttpStatus.BAD_REQUEST),
    SHIPMENT_NOT_READY(4006, HttpStatus.BAD_REQUEST),
    UNSUPPORTED_ORDER_STATUS(4007, HttpStatus.BAD_REQUEST),
    PAGE_MUST_BE_AT_LEAST_1(4008, HttpStatus.BAD_REQUEST),
    SIZE_MUST_BE_BETWEEN_1_AND_50(4009, HttpStatus.BAD_REQUEST),
    FORBIDDEN(4010, HttpStatus.FORBIDDEN),

    // ── 5xxx 购物车 ──────────────────────────────────────────
    CART_NOT_FOUND(5001, HttpStatus.NOT_FOUND),
    CART_ITEM_NOT_FOUND(5002, HttpStatus.NOT_FOUND),
    QUANTITY_MUST_BE_AT_LEAST_1(5003, HttpStatus.BAD_REQUEST),
    SELECTED_FLAG_REQUIRED(5004, HttpStatus.BAD_REQUEST),
    QUANTITY_EXCEEDS_STOCK(5005, HttpStatus.BAD_REQUEST),
    CART_ITEM_NOT_READY_FOR_CHECKOUT(5006, HttpStatus.BAD_REQUEST),
    INSUFFICIENT_STOCK(5007, HttpStatus.BAD_REQUEST),
    CART_ITEM_MISSING_PRODUCT_DATA(5008, HttpStatus.INTERNAL_SERVER_ERROR),

    // ── 6xxx 支付 ────────────────────────────────────────────
    PAYMENT_NOT_FOUND(6001, HttpStatus.NOT_FOUND),
    ONLY_PENDING_PAYMENT_CAN_CREATE_PAYMENT(6002, HttpStatus.BAD_REQUEST),
    ONLY_INITIATED_PAYMENT_CAN_SUCCEED(6003, HttpStatus.BAD_REQUEST),
    ORDER_ALREADY_PAID(6004, HttpStatus.BAD_REQUEST),
    SUCCESSFUL_PAYMENT_CANNOT_FAIL(6005, HttpStatus.BAD_REQUEST),
    ONLY_INITIATED_PAYMENT_CAN_FAIL(6006, HttpStatus.BAD_REQUEST),

    // ── 7xxx 文件与上传 ──────────────────────────────────────
    UPLOAD_FAILED(7001, HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_EMPTY(7002, HttpStatus.BAD_REQUEST),
    FILE_CATEGORY_BLANK(7003, HttpStatus.BAD_REQUEST),
    OSS_NOT_CONFIGURED(7004, HttpStatus.INTERNAL_SERVER_ERROR),

    // ── 8xxx 权限与日志 ──────────────────────────────────────
    ACCESS_DENIED(8001, HttpStatus.FORBIDDEN),
    ANONYMOUS_ID_REQUIRED(8002, HttpStatus.BAD_REQUEST),
    UNSUPPORTED_RECOMMENDATION_TYPE(8003, HttpStatus.BAD_REQUEST),
    PRODUCT_ID_REQUIRED_FOR_DETAIL_RECOMMENDATION(8004, HttpStatus.BAD_REQUEST);

    private final int code;
    private final HttpStatus httpStatus;

    ErrorCode(int code, HttpStatus httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public int code() {
        return code;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}

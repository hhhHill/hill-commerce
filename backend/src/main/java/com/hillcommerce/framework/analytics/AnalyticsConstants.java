package com.hillcommerce.framework.analytics;

public final class AnalyticsConstants {
    private AnalyticsConstants() {}

    /** 已完成/已付款的订单状态集合（SQL 字面量，用于 IN 子句 — 内部 SQL，无注入风险） */
    public static final String COMPLETED_ORDER_STATUS_SQL =
        "'PAID','SHIPPED','COMPLETED'";

    /** 平台级 shop_id */
    public static final long PLATFORM_SHOP_ID = 0L;
}

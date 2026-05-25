-- V12__analytics_enhancement.sql

-- 1. 预聚合表加 shop_id 字段（NOT NULL DEFAULT 0, 0=平台级）
ALTER TABLE hourly_sales_snapshot ADD COLUMN shop_id BIGINT NOT NULL DEFAULT 0 AFTER id;
ALTER TABLE hourly_sales_snapshot ADD INDEX idx_snapshot_shop (shop_id);
-- 调度器幂等：防止同一小时重复插入
ALTER TABLE hourly_sales_snapshot ADD UNIQUE KEY uk_snapshot_hour_shop (snapshot_hour, shop_id);

ALTER TABLE daily_sales_summary ADD COLUMN shop_id BIGINT NOT NULL DEFAULT 0 AFTER id;
ALTER TABLE daily_sales_summary DROP INDEX stat_date;
ALTER TABLE daily_sales_summary ADD UNIQUE KEY uk_date_shop (stat_date, shop_id);

ALTER TABLE product_sales_stats ADD COLUMN shop_id BIGINT NOT NULL DEFAULT 0 AFTER category_id;
ALTER TABLE product_sales_stats DROP INDEX uk_product_date;
ALTER TABLE product_sales_stats ADD UNIQUE KEY uk_product_date_shop (product_id, stat_date, shop_id);
ALTER TABLE product_sales_stats ADD INDEX idx_product_stats_shop (shop_id);

-- 2. 异常告警持久化表
CREATE TABLE anomaly_alerts (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_hour   DATETIME(3) NOT NULL,
    order_count     INT NOT NULL,
    total_amount    DECIMAL(18,2) NOT NULL,
    baseline_mean   DECIMAL(18,2) NOT NULL,
    baseline_std    DECIMAL(18,2) NOT NULL,
    direction       VARCHAR(10) NOT NULL COMMENT 'high / low',
    deviation_pct   DECIMAL(10,2) NOT NULL,
    shop_id         BIGINT NOT NULL DEFAULT 0,
    acknowledged    BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_by VARCHAR(100) NULL,
    acknowledged_at DATETIME(3) NULL,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_anomaly_snapshot (snapshot_hour),
    INDEX idx_anomaly_shop_ack (shop_id, acknowledged),
    UNIQUE KEY uk_anomaly_hour (snapshot_hour, shop_id, direction)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

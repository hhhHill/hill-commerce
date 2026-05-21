-- V8__merchant_platform.sql

-- 1. Create shops table
CREATE TABLE IF NOT EXISTS shops (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100)  NOT NULL,
    slug        VARCHAR(100)  NOT NULL,
    logo_url    VARCHAR(500),
    description TEXT,
    owner_id    BIGINT NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_shops_slug (slug),
    UNIQUE INDEX uk_shops_owner (owner_id),
    INDEX idx_shops_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Add shop_id to products
ALTER TABLE products ADD COLUMN shop_id BIGINT NULL AFTER category_id;
ALTER TABLE products ADD INDEX idx_products_shop (shop_id);

-- 3. Add shop_id to orders
ALTER TABLE orders ADD COLUMN shop_id BIGINT NULL AFTER user_id;
ALTER TABLE orders ADD INDEX idx_orders_shop (shop_id);

-- 4. Rename SALES role to MERCHANT
UPDATE roles SET code = 'MERCHANT', name = '商家' WHERE code = 'SALES';

CREATE TABLE product (
    id BIGINT PRIMARY KEY,
    product_name VARCHAR(128) NOT NULL,
    product_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE product_sku (
    id BIGINT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    sku_code VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE price (
    id BIGINT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    sale_price DECIMAL(12, 2) NOT NULL,
    currency_code VARCHAR(8) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

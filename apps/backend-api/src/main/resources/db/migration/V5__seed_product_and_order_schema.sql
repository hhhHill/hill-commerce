ALTER TABLE product
    ADD COLUMN sale_price DECIMAL(12, 2) NOT NULL DEFAULT 0.00;

ALTER TABLE order_item
    ADD COLUMN product_name VARCHAR(128) NOT NULL DEFAULT '',
    ADD COLUMN line_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00;

INSERT INTO product (id, product_name, product_status, created_at, updated_at, sale_price)
VALUES (1001, 'Java Course', 'ON_SHELF', NOW(), NOW(), 99.00),
       (1002, 'Spring Boot Course', 'ON_SHELF', NOW(), NOW(), 129.00),
       (1003, 'Archived Course', 'OFF_SHELF', NOW(), NOW(), 59.00);

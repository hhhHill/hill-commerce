CREATE TABLE roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_roles_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(128) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    last_login_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_roles_user_role UNIQUE (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_addresses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    receiver_name VARCHAR(64) NOT NULL,
    receiver_phone VARCHAR(32) NOT NULL,
    province VARCHAR(64) NOT NULL,
    city VARCHAR(64) NOT NULL,
    district VARCHAR(64) NOT NULL,
    detail_address VARCHAR(255) NOT NULL,
    postal_code VARCHAR(16) NULL,
    is_default TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT fk_user_addresses_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_user_addresses_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE product_categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_product_categories_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    spu_code VARCHAR(64) NOT NULL,
    subtitle VARCHAR(255) NULL,
    cover_image_url VARCHAR(512) NULL,
    description LONGTEXT NULL,
    min_sale_price DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(32) NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    deleted_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_products_spu_code UNIQUE (spu_code),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES product_categories (id),
    INDEX idx_products_category_id (category_id),
    INDEX idx_products_status_deleted (status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE product_images (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    image_url VARCHAR(512) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products (id),
    INDEX idx_product_images_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE product_sales_attributes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    attribute_name VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_sales_attributes_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT uk_product_sales_attr_name UNIQUE (product_id, attribute_name),
    INDEX idx_product_sales_attributes_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE product_sales_attribute_values (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    sales_attribute_id BIGINT NOT NULL,
    attribute_value VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_sales_attribute_values_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_product_sales_attribute_values_attr FOREIGN KEY (sales_attribute_id) REFERENCES product_sales_attributes (id),
    CONSTRAINT uk_product_sales_attr_value UNIQUE (sales_attribute_id, attribute_value),
    INDEX idx_product_sales_attribute_values_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE product_attribute_values (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    attribute_name VARCHAR(64) NOT NULL,
    attribute_value VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_attribute_values_product FOREIGN KEY (product_id) REFERENCES products (id),
    INDEX idx_product_attribute_values_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE product_skus (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    sku_code VARCHAR(64) NOT NULL,
    sales_attr_value_key VARCHAR(255) NOT NULL,
    sales_attr_value_text VARCHAR(255) NOT NULL,
    price DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
    stock INT NOT NULL DEFAULT 0,
    low_stock_threshold INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    deleted_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_product_skus_sku_code UNIQUE (sku_code),
    CONSTRAINT uk_product_skus_attr_key UNIQUE (product_id, sales_attr_value_key),
    CONSTRAINT fk_product_skus_product FOREIGN KEY (product_id) REFERENCES products (id),
    INDEX idx_product_skus_product_id (product_id),
    INDEX idx_product_skus_status_deleted (status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE carts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_carts_user_id UNIQUE (user_id),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE cart_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    selected TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_cart_items_cart_sku UNIQUE (cart_id, sku_id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id),
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_cart_items_sku FOREIGN KEY (sku_id) REFERENCES product_skus (id),
    INDEX idx_cart_items_product_id (product_id),
    INDEX idx_cart_items_sku_id (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    order_status VARCHAR(32) NOT NULL,
    total_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
    payable_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
    payment_deadline_at DATETIME(3) NOT NULL,
    paid_at DATETIME(3) NULL,
    shipped_at DATETIME(3) NULL,
    completed_at DATETIME(3) NULL,
    cancelled_at DATETIME(3) NULL,
    cancel_reason VARCHAR(255) NULL,
    address_snapshot_name VARCHAR(64) NOT NULL,
    address_snapshot_phone VARCHAR(32) NOT NULL,
    address_snapshot_province VARCHAR(64) NOT NULL,
    address_snapshot_city VARCHAR(64) NOT NULL,
    address_snapshot_district VARCHAR(64) NOT NULL,
    address_snapshot_detail VARCHAR(255) NOT NULL,
    address_snapshot_postal_code VARCHAR(16) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_orders_order_no UNIQUE (order_no),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_orders_user_id (user_id),
    INDEX idx_orders_status (order_status),
    INDEX idx_orders_payment_deadline_at (payment_deadline_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    product_name_snapshot VARCHAR(255) NOT NULL,
    sku_code_snapshot VARCHAR(64) NOT NULL,
    sku_attr_text_snapshot VARCHAR(255) NOT NULL,
    product_image_snapshot VARCHAR(512) NULL,
    unit_price DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
    quantity INT NOT NULL,
    subtotal_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_order_items_sku FOREIGN KEY (sku_id) REFERENCES product_skus (id),
    INDEX idx_order_items_order_id (order_id),
    INDEX idx_order_items_product_id (product_id),
    INDEX idx_order_items_sku_id (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE order_status_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    changed_by BIGINT NULL,
    change_reason VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT fk_order_status_histories_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_status_histories_user FOREIGN KEY (changed_by) REFERENCES users (id),
    INDEX idx_order_status_histories_order_id (order_id),
    INDEX idx_order_status_histories_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    payment_no VARCHAR(64) NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    payment_status VARCHAR(32) NOT NULL,
    amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
    requested_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    paid_at DATETIME(3) NULL,
    fail_reason VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_payments_payment_no UNIQUE (payment_no),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id),
    INDEX idx_payments_order_id (order_id),
    INDEX idx_payments_status (payment_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE shipments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    carrier_name VARCHAR(64) NULL,
    tracking_no VARCHAR(128) NULL,
    shipment_status VARCHAR(32) NOT NULL,
    remark VARCHAR(255) NULL,
    operated_by BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_shipments_order_id UNIQUE (order_id),
    CONSTRAINT fk_shipments_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_shipments_operator FOREIGN KEY (operated_by) REFERENCES users (id),
    INDEX idx_shipments_status (shipment_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE login_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    email_snapshot VARCHAR(128) NOT NULL,
    role_snapshot VARCHAR(64) NOT NULL,
    login_result VARCHAR(32) NOT NULL,
    ip_address VARCHAR(64) NOT NULL,
    user_agent VARCHAR(512) NULL,
    login_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT fk_login_logs_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_login_logs_login_at (login_at),
    INDEX idx_login_logs_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE operation_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    operator_user_id BIGINT NOT NULL,
    operator_role VARCHAR(64) NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(64) NOT NULL,
    action_detail VARCHAR(255) NOT NULL,
    ip_address VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT fk_operation_logs_operator FOREIGN KEY (operator_user_id) REFERENCES users (id),
    INDEX idx_operation_logs_operator_user_id (operator_user_id),
    INDEX idx_operation_logs_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE product_view_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    anonymous_id VARCHAR(128) NULL,
    product_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    viewed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT fk_product_view_logs_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_product_view_logs_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_product_view_logs_category FOREIGN KEY (category_id) REFERENCES product_categories (id),
    INDEX idx_product_view_logs_viewed_at (viewed_at),
    INDEX idx_product_view_logs_user_id (user_id),
    INDEX idx_product_view_logs_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE mail_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    biz_type VARCHAR(64) NOT NULL,
    biz_id VARCHAR(64) NOT NULL,
    recipient_email VARCHAR(128) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    send_status VARCHAR(32) NOT NULL,
    sent_at DATETIME(3) NULL,
    fail_reason VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_mail_logs_biz (biz_type, biz_id),
    INDEX idx_mail_logs_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

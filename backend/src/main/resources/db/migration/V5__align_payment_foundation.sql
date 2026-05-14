ALTER TABLE payments
    ADD COLUMN user_id BIGINT NULL AFTER order_id,
    ADD COLUMN closed_at DATETIME(3) NULL AFTER paid_at;

CREATE INDEX idx_orders_status_deadline ON orders (order_status, payment_deadline_at);
CREATE INDEX idx_payments_order_status ON payments (order_id, payment_status);

ALTER TABLE shipments
ADD INDEX idx_shipments_order_id (order_id),
DROP INDEX uk_shipments_order_id;

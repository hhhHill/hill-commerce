-- V13__product_operation_log.sql
-- 为 operation_logs 表添加商品操作日志字段
-- 用于记录操作时的快照信息和字段级变更追踪

ALTER TABLE operation_logs
  ADD COLUMN target_name VARCHAR(255) NULL AFTER target_id,
  ADD COLUMN target_spu_code VARCHAR(128) NULL AFTER target_name,
  ADD COLUMN field_changes JSON NULL AFTER action_detail;

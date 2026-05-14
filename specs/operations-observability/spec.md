# Feature Specification: operations-observability

**Feature**: `operations-observability`  
**Status**: active  
**Parent Baseline**: `specs/hill-commerce-mvp/spec.md`

## Purpose

定义日志、邮件通知、基础统计、联调与上线准备相关的运维与可观测性范围。

## Scope

### In Scope

- 登录日志
- 后台操作日志
- 商品浏览日志
- 邮件通知与发送记录
- Dashboard 基础统计
- 联调、异常页、演示链路验证

### Out of Scope

- 高级分析系统
- 大屏可视化
- 实时行为分析
- 复杂告警平台

## Roles

### Sales

- 查看商品浏览日志
- 查看自身及下属后台操作日志
- 接收订单、发货相关邮件通知

### Admin

- 查看所有用户登录日志
- 查看销售统计报表（按类别、状态、库存）
- 监控 Sales 操作日志
- 管理邮件通知配置

## Business Rules

- 登录成功与失败都应有日志
- 后台关键写操作应有操作日志
- 邮件发送失败不得阻断主交易流程
- Dashboard 只提供基础统计，不承担高级分析职责

## Acceptance Criteria

- 日志可查询
- 邮件链路可追踪
- 基础统计可查看
- 主流程联调和异常路径具备最小可演示性

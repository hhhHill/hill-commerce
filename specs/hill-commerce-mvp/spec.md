# Feature Specification: hill-commerce-mvp

**Feature**: `hill-commerce-mvp`  
**Status**: Canonical Baseline  
**Source**: Migrated and normalized from `docs/superpowers/specs/2026-05-09-hill-commerce-mvp-design.md`

## Purpose

`hill-commerce-mvp` 是当前商城 MVP 的 canonical baseline。

它用于定义：

- 项目目标
- MVP 范围
- 用户角色
- 全局非目标
- 核心业务规则
- 跨模块约束
- 验收标准

它不应长期承载过细的模块实现细节；复杂模块后续应按拆分条件独立为 `specs/<feature-name>/spec.md`。

## Product Goal

构建一个面向实体商品销售的商城 MVP，完成从商品浏览、用户注册登录、购物车、下单、模拟支付，到后台商品管理、发货、日志与基础统计的完整演示闭环。

## MVP Scope

### In Scope

- 用户注册、登录、注销
- 前台商品浏览、搜索、详情查看
- 登录用户购物车、地址管理、下单
- 模拟支付
- 未支付订单取消与超时关闭
- Sales 后台商品管理、订单发货
- Admin 账号权限管理、日志与基础统计
- 登录日志、后台操作日志、商品浏览日志
- 邮件通知基础闭环
- Redis 与 RocketMQ 的架构预留

### Out of Scope

- 数字商品、服务商品
- 真实支付渠道
- 在线退款
- 售后正式流程
- 多级分类
- 复杂搜索与 Elasticsearch
- 匿名购物车与登录后购物车合并
- 正式推荐系统
- 高级报表与可视化大屏
- 数据导入导出
- 移动端 App

## Roles

### Customer

可注册、登录、浏览商品、管理购物车和地址、提交订单、完成模拟支付、查看订单和物流、确认收货。

### Sales

可登录后台，管理一级分类、商品、SKU、库存、订单与发货信息。

### Admin

可登录后台，管理 Sales 账号与角色，查看日志与基础统计。

## Global Business Rules

### Authentication and Roles

- 后端是唯一认证事实源
- 浏览器通过 Session Cookie 维持登录态
- 前台注册用户默认授予 `CUSTOMER`
- 后台至少支持 `ADMIN` 与 `SALES` 角色边界

### Product and Catalog

- 仅支持一级分类
- 商品采用 `SPU + SKU` 模型
- 每个商品最多支持 2 个销售属性
- 商品支持封面图、详情图、富文本描述
- 商品允许上架、下架、逻辑删除
- SKU 可自动生成编码，也允许手工修改

### Cart and Checkout

- 仅登录用户可加入购物车
- 不支持匿名购物车
- 不支持登录后购物车合并
- 购物车项以 `sku_id` 为核心
- 下单前必须再次校验商品状态、SKU 可售性与库存

### Order, Payment, Shipping

- 库存采用“下单即扣减”
- 未支付订单允许手动取消
- 未支付订单 30 分钟自动关闭并回补库存
- 首版支付为模拟支付
- 支付成功后订单进入待发货
- Sales 发货后订单进入已发货
- 用户确认收货或超时后订单完成

### Observability and Extension

- 登录日志、操作日志、商品浏览日志属于 MVP 范围
- 邮件发送失败不得阻塞主交易流程
- Redis 与 RocketMQ 只做预留，不得成为 MVP 主链路运行前提

## Cross-Module Constraints

- 金额字段必须使用定点数
- 状态字段必须使用受控枚举
- 订单、支付、发货等关键状态流转必须可审计
- 关键写路径要考虑幂等与前置状态校验
- 前端展示数据不能覆盖服务端最终校验结果

## Candidate Future Feature Specs

以下模块满足优先拆分候选：

- `order`
- `payment`
- `inventory`
- `cart`
- `checkout`
- `promotion`
- `refund-after-sales`
- `auth-permission`
- `admin-product-management`

## Acceptance Criteria

### Functional

- 用户可注册、登录、浏览商品、加购、下单、模拟支付、查看订单并确认收货
- Sales 可维护分类、商品、SKU、库存和发货信息
- Admin 可管理后台账号权限并查看日志和基础统计
- 未支付订单可手动取消，并能自动超时关闭
- 日志、邮件、基础统计具备最小可用性

### Non-Functional

- 系统可本地稳定启动
- 数据库变更可通过迁移执行
- 主流程不依赖 Redis 或 RocketMQ
- 前后台权限隔离有效
- 可完成完整演示链路：浏览、注册登录、加购、下单、支付、发货、收货

## Notes

- 订单状态机、支付状态机、库存策略、退款售后、促销规则不应长期塞在 baseline 中；当规则复杂度提升时应拆为独立 feature spec

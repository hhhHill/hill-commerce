# Feature Specification: hill-commerce-mvp

**Feature**: `hill-commerce-mvp`  
**Status**: active  
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

当前第一轮已确认的 phase-based feature 分流目标包括：

- `auth-permission`
- `admin-product-management`
- `shopping-journey`
- `order-checkout`
- `payment`
- `fulfillment`
- `operations-observability`

## Product Goal

构建一个面向实体商品销售的商城 MVP，完成从商品浏览、用户注册登录、购物车、下单、模拟支付，到后台商品管理、发货、日志与基础统计的完整演示闭环。

## Product Positioning

`hill-commerce` 是一个面向实体商品销售的电子商务网站 MVP。

第一阶段目标不是覆盖所有商城能力，而是优先打通以下两条主链路：

- 前台交易闭环：注册登录、浏览商品、购物车、下单、模拟支付、查单、收货
- 后台管理闭环：分类管理、商品管理、库存维护、订单发货、日志与基础统计

## Design Principles

- 不过度设计，优先保证主链路完整
- 先支持实体商品，不支持数字商品和服务商品
- 架构采用模块化单体，便于首版快速交付
- 支付、推荐、分析先做简化版或预留扩展点
- 所有设计以后续接真实支付、推荐系统和数据分析为前提，但首版不提前实现复杂功能
- Redis 和 RocketMQ 在架构上预留接入点，但不作为 MVP 主链路运行前提

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

## Account System

- MVP 采用统一账号体系
- 所有角色共用 `users` 表
- 通过 `roles` 与 `user_roles` 进行角色区分
- 不单独拆分 `sales_accounts`、`admin_accounts`

## Global Business Rules

### Authentication and Roles

- 后端是唯一认证事实源
- 浏览器通过 Session Cookie 维持登录态
- 前台注册用户默认授予 `CUSTOMER`
- 后台至少支持 `ADMIN` 与 `SALES` 角色边界
- 邮箱注册后首版不支持修改邮箱
- 登录成功与失败都必须记录应用日志，且禁止记录明文密码

### Product and Catalog

- 仅支持一级分类
- 商品采用 `SPU + SKU` 模型
- 每个商品最多支持 2 个销售属性
- 商品支持封面图、详情图、富文本描述
- 商品允许上架、下架、逻辑删除
- SKU 可自动生成编码，也允许手工修改
- 后台商品管理采用一次提交的聚合保存模型
- 商品详情默认展示最低 SKU 价格起

### Cart and Checkout

- 仅登录用户可加入购物车
- 不支持匿名购物车
- 不支持登录后购物车合并
- 购物车项以 `sku_id` 为核心
- 下单前必须再次校验商品状态、SKU 可售性与库存
- 地址快照必须在订单创建时固化，不能依赖后续地址表变更

### Order, Payment, Shipping

- 库存采用“下单即扣减”
- 未支付订单允许手动取消
- 未支付订单 30 分钟自动关闭并回补库存
- 首版支付为模拟支付
- 支付成功后订单进入待发货
- Sales 发货后订单进入已发货
- 用户确认收货或超时后订单完成
- 发货后 10 天未确认收货时，系统可自动完成订单
- 发货信息首版只要求快递公司与运单号录入，不接真实物流接口

### Observability and Extension

- 登录日志、操作日志、商品浏览日志属于 MVP 范围
- 邮件发送失败不得阻塞主交易流程
- Redis 与 RocketMQ 只做预留，不得成为 MVP 主链路运行前提
- Redis 预留用途包括商品缓存、热门商品、简单限流与会话扩展能力
- RocketMQ 预留用途包括订单、支付、发货、邮件、埋点与统计异步事件

## Cross-Module Constraints

- 金额字段必须使用定点数
- 状态字段必须使用受控枚举
- 订单、支付、发货等关键状态流转必须可审计
- 关键写路径要考虑幂等与前置状态校验
- 前端展示数据不能覆盖服务端最终校验结果
- 首版所有关键业务流程必须在未启用 Redis / RocketMQ 的条件下稳定运行

## Page Inventory

### Frontend

- 首页
- 分类商品列表页
- 商品详情页
- 登录页
- 注册页
- 购物车页
- 结算页
- 下单成功页
- 我的订单列表页
- 订单详情页
- 收货地址管理页
- 个人中心基础页

### Admin

- 后台登录页
- Dashboard 页
- 分类管理页
- 商品列表管理页
- 商品新增页
- 商品编辑页
- 订单列表页
- 订单详情页
- 发货处理页
- 用户角色管理页
- 登录日志页
- 操作日志页
- 基础统计页

### Common Error Pages

- `403`
- `404`
- `500`

## State Model Summary

### Order

- `PENDING_PAYMENT`
- `CANCELLED`
- `PAID`
- `SHIPPED`
- `COMPLETED`

关键约束：

- 只有 `PENDING_PAYMENT` 可取消
- 只有 `PAID` 可发货
- 只有 `SHIPPED` 可确认收货或自动完成
- `CANCELLED` 与 `COMPLETED` 为终态

### Payment

- `PENDING`
- `SUCCESS`
- `FAILED`

关键约束：

- 同一支付流水不允许从 `SUCCESS` 回退
- 首版按单次成功支付闭环处理，不做拆单支付和部分支付

### Shipment

- `PENDING`
- `SHIPPED`
- `DELIVERED`

## Current Repository Reality

当前仓库已经存在一部分实现骨架，这会影响后续迁移和实施节奏：

- 后端当前已落地 `framework`、`user`、`product`、`admin` 的基础实现
- 前端当前已落地认证页、`/account`、`/admin`、后台分类和商品管理页面骨架
- `cart`、`order`、`payment`、`shipping`、`audit`、`notification` 仍主要处于规范定义阶段，而不是完整实现阶段

这意味着本文件定义的是当前 canonical baseline，而不是对现有代码完成度的背书。

## Current Feature Split Map

当前 baseline 与第一轮 phase-based feature 的分流关系如下：

- `auth-permission`: 认证、会话、角色边界、受保护页面
- `admin-product-management`: 后台分类、SPU、SKU、商品状态管理
- `shopping-journey`: 前台浏览、搜索、购物车、地址管理
- `order-checkout`: 结算、订单创建、快照、库存扣减、取消
- `payment`: 模拟支付、支付流水、超时关闭
- `fulfillment`: 发货、物流、确认收货、自动完成
- `operations-observability`: 日志、邮件、统计、联调与上线准备

## Candidate Future Feature Specs

若后续复杂度继续上升，仍可进一步拆出更细粒度 feature，例如：

- `inventory`
- `promotion`
- `refund-after-sales`

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
- 当前 baseline 允许保留适量“全局规则”与“跨模块约束”，但不应继续吸纳细粒度实现策略

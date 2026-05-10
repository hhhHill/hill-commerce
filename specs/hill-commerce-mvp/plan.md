# Implementation Plan: hill-commerce-mvp

**Feature**: `hill-commerce-mvp`  
**Status**: Canonical Baseline  
**Source**: Migrated and normalized from `docs/superpowers/plans/2026-05-09-hill-commerce-mvp.md`

## Summary

`hill-commerce-mvp` 采用模块化单体架构，目标是在不引入微服务复杂度的前提下，完成商城核心交易闭环和后台管理闭环，并为 Redis、RocketMQ、正式支付、推荐等后续能力预留扩展点。

## Technical Context

- Backend: Java 21, Spring Boot 4, Spring Security 7, MyBatis-Plus, Flyway
- Frontend: Next.js 15, React 19, TypeScript, Tailwind CSS 4
- Database: MySQL 9.7 LTS
- Infra Reserve: Redis 8, RocketMQ
- Deployment: Docker Compose, Nginx
- Testing: JUnit 5, Spring Boot Test, Testcontainers, frontend typecheck/build

## Architecture

### System Shape

- 项目采用前后端分离开发
- 后端为模块化单体
- 前端通过 Next.js 构建前台与后台管理页面
- 首版不做微服务，不做真实支付，不把 Redis / RocketMQ 绑进主链路

### Backend Module Boundaries

- `framework`: 安全、Web、Flyway、基础设施抽象
- `user`: 注册登录、角色、地址、用户资料
- `product`: 分类、SPU、SKU、销售属性、展示属性、图片
- `cart`: 购物车与结算前校验
- `order`: 订单创建、订单项快照、取消、自动关闭、确认收货、自动完成
- `payment`: 模拟支付、支付流水
- `shipping`: 发货与物流
- `admin`: 后台入口、角色权限、基础统计聚合
- `audit`: 登录日志、操作日志、浏览日志
- `notification`: 邮件通知与发送日志
- `framework/cache`: 缓存抽象与 Key 规范预留
- `framework/events`: 本地事件发布器与 RocketMQ 扩展位预留

### Frontend Boundaries

- `(auth)`: 登录注册
- `(shop)`: 前台浏览、详情、购物车、地址、结算、订单
- `admin`: 后台分类、商品、订单、发货、日志、统计
- `lib/auth`: 同源代理、服务端鉴权辅助、角色边界

## Data Model Intent

### Core Aggregates

- `users / roles / user_roles`: 统一账号体系
- `products / product_skus / product_categories`: 商品与库存基础
- `carts / cart_items`: 购物车
- `orders / order_items / order_status_histories`: 订单与状态追踪
- `payments`: 支付流水
- `shipments`: 发货与物流
- `login_logs / operation_logs / product_view_logs / mail_logs`: 审计与通知

### Key Data Rules

- `email`、`spu_code`、`sku_code`、`order_no`、`payment_no` 等关键字段必须唯一
- 同一购物车内同一 `sku_id` 只能有一条记录
- 订单必须保存商品与地址快照
- 金额字段使用定点数
- 逻辑删除与业务状态分离

## API Groups

- `/api/auth/*`: 注册、登录、注销、当前用户
- `/api/user/*`: 地址与用户相关接口
- `/api/categories`、`/api/products/*`: 前台商品浏览
- `/api/cart/*`: 购物车
- `/api/orders/*`: 下单、查单、取消、确认收货
- `/api/payments/*`: 模拟支付与支付状态
- `/api/admin/categories/*`: 后台分类管理
- `/api/admin/products/*`: 后台商品管理
- `/api/admin/orders/*`: 后台订单与发货
- `/api/admin/users/*`: 后台账号与角色管理
- `/api/admin/logins`、`/api/admin/operations`、`/api/admin/dashboard/summary`: 日志与统计

## Key Technical Constraints

- 后端是唯一认证事实源
- Spring Security 使用基于 Session 的认证，不依赖 Redis 才能闭环
- Next.js `middleware.ts` 仅做快速拦截，不在 middleware 中请求后端
- 前端通过同源 Route Handlers 代理认证请求，保证 Cookie 策略一致
- 关键状态变更必须做前置状态校验
- 下单、取消、超时关闭、支付等流程必须考虑幂等
- 缓存只做加速层，不做首版唯一数据源
- 本地同步事件实现必须与未来 RocketMQ 替换边界兼容

## README References

以下内容继续以 `README.md` 为准，不整体迁入本文件：

- 本地安装说明
- 启动命令
- 环境变量样例
- 日常开发入口说明

仅以下约束被吸收为 canonical implementation context：

- 当前技术栈
- Redis / RocketMQ 仅预留，不作为主流程前提
- 本地开发与演示优先

## Split Guidance

当下列主题复杂度提升时，应优先拆成独立 feature plan：

- `order`
- `payment`
- `inventory`
- `checkout`
- `auth-permission`
- `admin-product-management`

## Verification Expectations

- 文档迁移完成后，旧 superpowers spec/plan 需加 deprecated notice
- 后续所有中高复杂度开发默认从本文件和对应 feature spec 开始
- 若发现代码与本计划冲突，先在 `specs/migration-map.md` 记录 `needs-decision`

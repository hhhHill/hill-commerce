# Implementation Plan: hill-commerce-mvp

**Feature**: `hill-commerce-mvp`  
**Status**: active  
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

## Current Repository Status

当前仓库不是空白起点，而是已经完成了部分 MVP 骨架与若干纵切片实现：

- 后端已有应用启动骨架、健康检查、Flyway 基线、认证与后台商品管理相关实现
- 前端已有认证代理、受保护页面、后台分类与商品管理页面骨架
- Docker Compose 已包含 `mysql`、`redis`、`mailhog`，并为 `backend`、`frontend`、`nginx` 提供 `app` profile

因此，本文件描述的是“当前 canonical MVP 技术方案 + 后续实现边界”，不是旧计划的原样复制。

## Current Split Strategy

当前不直接把 MVP baseline 拆成大量技术模块，而是先按执行 phase 形成第一轮 feature 分流：

- `auth-permission`
- `admin-product-management`
- `shopping-journey`
- `order-checkout`
- `payment`
- `fulfillment`
- `operations-observability`

本文件继续作为项目级 MVP 技术总纲，不负责替代这些 feature 的独立细化空间。

## Architecture

### System Shape

- 项目采用前后端分离开发
- 后端为模块化单体
- 前端通过 Next.js 构建前台与后台管理页面，并通过同源 Route Handlers 代理后端认证请求
- 首版不做微服务，不做真实支付，不把 Redis / RocketMQ 绑进主链路

### Backend Module Boundaries

- 当前已落地：
- `framework`: 安全、Web、异常处理、健康检查、基础设施抽象
- `framework/cache`: 缓存接口与 Noop 实现预留
- `framework/events`: 本地领域事件发布器预留
- `user`: 注册登录、角色、认证、基础用户模型
- `product`: 后台分类、SPU、SKU、销售属性、展示属性、图片
- `admin`: 后台认证入口

- 目标模块边界仍定义为：
- `cart`: 购物车与结算前校验
- `order`: 订单创建、订单项快照、取消、超时关闭、确认收货、自动完成
- `payment`: 模拟支付、支付流水
- `shipping`: 发货与物流
- `audit`: 登录日志、操作日志、浏览日志
- `notification`: 邮件通知与发送日志

### Backend Package Reality

当前后端实际包路径以 `backend/src/main/java/com/hillcommerce/` 为根，而不是旧设计中的抽象 `common/framework/modules` 顶层展示图直接落盘。

在当前仓库里，更贴近现实的结构是：

- `framework/cache`
- `framework/events`
- `framework/security`
- `framework/web`
- `modules/admin`
- `modules/product`
- `modules/user`

后续新增模块应延续这一结构，而不是重新发明新的顶层目录。

### Frontend Boundaries

- 当前已落地：
- `src/app/(auth)`: 登录注册
- `src/app/account`: 最小前台受保护页面
- `src/app/admin`: 后台首页、分类管理、商品管理
- `src/app/api/auth/*`: 登录、注册、注销、当前用户同源代理
- `src/app/api/admin/[...path]`: 后台接口代理
- `src/features/admin/catalog`: 后台分类与商品管理 UI
- `src/lib/auth`: 同源代理、服务端鉴权辅助、角色边界
- `src/lib/admin`: 后台接口客户端与服务端访问辅助

- 目标前端范围仍包括：
- 商品浏览页
- 购物车与结算页
- 订单列表与订单详情页
- 发货、日志、统计相关后台页

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
- 当前数据库基线已经通过 Flyway 管理，后续扩展必须延续 `backend/src/main/resources/db/migration/` 下的迁移策略

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

说明：

- 当前代码已经落地 `/api/auth/*` 与 `/api/admin/*` 的一部分接口和代理
- `cart`、`orders`、`payments`、发货与统计中的大部分接口目前仍属于 canonical target，而不是已完成事实

## Key Technical Constraints

- 后端是唯一认证事实源
- Spring Security 使用基于 Session 的认证，不依赖 Redis 才能闭环
- Next.js `middleware.ts` 仅做快速拦截，不在 middleware 中请求后端
- 前端通过同源 Route Handlers 代理认证请求，保证 Cookie 策略一致
- 关键状态变更必须做前置状态校验
- 下单、取消、超时关闭、支付等流程必须考虑幂等
- 缓存只做加速层，不做首版唯一数据源
- 本地同步事件实现必须与未来 RocketMQ 替换边界兼容
- 当前目录和实现骨架已经存在，后续实现应在现有目录上增量扩展，而不是按旧计划重建工程骨架

## Repository Layout References

与当前实现直接相关的关键目录如下：

- `backend/`
- `backend/src/main/java/com/hillcommerce/`
- `backend/src/main/resources/db/migration/`
- `frontend/next-app/`
- `frontend/next-app/src/app/`
- `frontend/next-app/src/features/`
- `frontend/next-app/src/lib/`
- `ops/nginx/`
- `docker-compose.yml`

以下旧文档路径仅用于历史迁移参考，不再作为规范事实源：

- `docs/superpowers/specs/2026-05-09-hill-commerce-mvp-design.md`
- `docs/superpowers/plans/2026-05-09-hill-commerce-mvp.md`

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
- 当前仓库已存在 Docker Compose、Flyway 基线、认证与后台商品管理骨架

## Module Delivery Reality

结合当前仓库状态，MVP 模块可分为三层：

### 已有可复用骨架

- 应用启动与健康检查
- Flyway 数据库基线
- Session 认证与角色边界
- 后台分类与商品管理基础骨架
- 缓存与事件抽象预留

### 已定义规范但待继续实现

- 购物车
- 地址管理
- 下单与库存扣减
- 模拟支付
- 发货与物流
- 日志、邮件、统计

### 当前第一轮已拆分或准备拆分的 feature

- `auth-permission`
- `admin-product-management`
- `shopping-journey`
- `order-checkout`
- `payment`
- `fulfillment`
- `operations-observability`

### 后续按复杂度继续细拆的候选

- `inventory`
- `promotion`
- `refund-after-sales`

## Split Guidance

当下列主题复杂度提升时，应优先拆成独立 feature plan：

- `inventory`
- `promotion`
- `refund-after-sales`
- 以及任何已经拆出的 phase-based feature 内继续膨胀的高风险子主题

## Verification Expectations

- 文档迁移完成后，旧 superpowers spec/plan 需加 deprecated notice
- 后续所有中高复杂度开发默认从本文件和对应 feature spec 开始
- 若发现代码与本计划冲突，先在 `specs/migration-map.md` 记录 `needs-decision`
- 若未来真实目录结构演化，与本文件不一致时，应先修订本文件，再继续扩展实现

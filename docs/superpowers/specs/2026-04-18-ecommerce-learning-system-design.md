# hill-commerce 总体架构设计

## 1. 概述

本项目是一个面向电商学习场景的单体模块化系统，目标是在保持单体应用开发效率的同时，具备清晰的模块边界，以及向高并发、异步化、服务化演进的能力。

已确认的核心约束如下：

- 架构风格：严格模块化单体
- 构建方式：Maven 多模块聚合
- 后端技术栈：Spring Boot + MySQL + Redis + Flyway
- 前端组织：同仓分应用，包含 `web` 与 `admin`
- 交互风格：同步主交易链路 + 异步事件扩展链路
- 模块协作规则：模块之间只能依赖对方的 `api`
- 当前非目标：微服务、分布式事务、复杂推荐算法、真实支付网关集成、多商户支持

## 2. 目标

本次架构设计与首版骨架的目标如下：

- 提供一个可直接扩展的后端工程底座
- 保证各业务模块边界清晰、依赖单向、职责稳定
- 为后续引入消息队列和高并发优化预留标准扩展点
- 为前后端分离协作提供统一仓库结构
- 通过 Flyway 统一数据库演进方式

## 3. 总体架构

系统采用单体部署、模块化开发的方式，整体分为三层：

1. 应用入口层：`apps`
2. 平台能力层：`platforms`
3. 业务模块层：`modules`

总体依赖方向如下：

```text
apps/backend-api
  -> platforms/*
  -> modules/*

modules/A
  -> platforms/*
  -> modules/B/api

platforms/*
  -> 不依赖具体业务模块
```

设计原则：

- 应用入口不承载业务规则，只负责装配和对外暴露 HTTP 接口
- 平台层提供通用能力，不含业务语义
- 业务模块封装各自领域模型、应用编排和基础设施实现
- 模块之间只通过显式 API 边界协作

## 4. 仓库结构

建议仓库结构如下：

```text
hill-commerce/
├─ pom.xml
├─ README.md
├─ .gitignore
├─ docs/
│  └─ superpowers/
│     ├─ specs/
│     └─ plans/
├─ apps/
│  ├─ backend-api/
│  ├─ web/
│  └─ admin/
├─ platforms/
│  ├─ common-core/
│  ├─ common-web/
│  ├─ common-security/
│  ├─ common-mysql/
│  ├─ common-redis/
│  ├─ common-event/
│  └─ common-test/
├─ modules/
│  ├─ user/
│  ├─ auth/
│  ├─ product/
│  ├─ cart/
│  ├─ order/
│  ├─ analytics/
│  └─ recommendation/
└─ db/
   └─ migration/
```

### 4.1 应用入口层

- `apps/backend-api`：当前唯一 Spring Boot 启动应用，负责 Controller、配置装配、统一异常处理、安全过滤器接入
- `apps/web`：用户端 Web 应用
- `apps/admin`：管理后台应用

### 4.2 平台能力层

- `common-core`：通用返回体、基础异常、上下文接口、ID/时间抽象
- `common-web`：Web 层公共配置、统一错误响应、TraceId 支持
- `common-security`：JWT、安全配置、鉴权基础设施
- `common-mysql`：持久化公共配置
- `common-redis`：缓存与 Redis 基础设施
- `common-event`：领域事件接口、本地事件总线、后续 MQ 扩展接口
- `common-test`：公共测试基座

### 4.3 业务模块层

每个业务模块至少具备如下结构：

```text
modules/order/
├─ pom.xml
└─ src/main/java/com/xxx/order/
   ├─ api/
   ├─ application/
   ├─ domain/
   └─ infrastructure/
```

职责定义：

- `api`：暴露给其他模块使用的 DTO、查询对象、Facade 接口、事件定义
- `application`：用例编排、事务、权限校验、调用领域对象、发布事件
- `domain`：聚合、实体、值对象、领域服务、业务规则
- `infrastructure`：数据库、缓存、消息、第三方组件、Repository 实现

## 5. 模块边界

### 5.1 边界规则

- 模块外只能访问目标模块的 `api`
- 禁止跨模块直接访问对方的 `domain`、`application`、`infrastructure`
- `platforms` 禁止依赖 `modules`
- `application` 层不直接暴露给模块外部
- 所有 HTTP Controller 统一放在 `apps/backend-api`

### 5.2 模块关系

- `user`：基础用户资料与收货地址能力
- `auth`：登录认证、JWT、角色权限，不承载用户业务资料
- `product`：商品、SKU、价格查询能力
- `cart`：购物车维护，依赖 `product/api`
- `order`：订单创建、状态流转、支付记录，依赖 `user/api`、`product/api`、`cart/api`
- `analytics`：消费行为与业务事件，做采集与聚合
- `recommendation`：基于行为日志或聚合结果生成推荐，不参与主交易链路

## 6. 运行时交互模型

### 6.1 同步交易链路

以下链路采用同步调用：

```text
浏览商品 -> 加购物车 -> 创建订单 -> 支付
```

同步链路原则：

- 主交易场景必须快速得到明确结果
- 同步链路优先保证事务清晰、失败可回滚、错误可追踪
- 订单创建阶段可同步读取用户、商品、购物车信息
- 支付在第一版中作为 `order` 模块内部能力存在，通过 `PaymentRecord` 记录结果

### 6.2 异步事件链路

以下场景采用事件驱动：

- 商品浏览
- 商品点击
- 加入购物车
- 订单创建成功
- 支付完成

事件链路如下：

```text
用户行为/业务完成
  -> 领域事件
  -> analytics 采集/聚合
  -> recommendation 消费统计结果或事件
```

### 6.3 事件策略

第一版使用本地事件总线实现，但必须从接口层面为未来引入消息队列做准备：

- 定义统一的 `DomainEvent` 抽象
- 定义统一的 `DomainEventPublisher`
- 默认实现为进程内发布
- 后续可扩展为 RabbitMQ、Kafka、Redis Stream
- 关键交易事件预留 Outbox 扩展点

## 7. 数据设计

### 7.1 数据库策略

- 使用 MySQL 作为主数据库
- 使用 Flyway 管理数据库变更
- 所有表按业务模块归属管理

建议的 Flyway 版本脚本：

- `V1__init_user_and_auth.sql`
- `V2__init_product.sql`
- `V3__init_cart_and_order.sql`
- `V4__init_analytics.sql`

### 7.2 表命名

- `user`
- `user_address`
- `role`
- `permission`
- `user_role`
- `role_permission`
- `product`
- `product_sku`
- `price`
- `cart`
- `cart_item`
- `orders`
- `order_item`
- `payment_record`
- `user_behavior_log`

说明：

- 订单表使用 `orders`，避免 `order` 关键字冲突

### 7.3 数据归属

- `user` 模块拥有用户与地址数据
- `auth` 模块拥有角色与权限映射数据
- `product` 模块拥有商品与价格数据
- `cart` 模块拥有购物车状态数据
- `order` 模块拥有订单、订单明细、支付记录数据
- `analytics` 模块拥有行为日志数据

## 8. 缓存策略

使用 Redis 作为缓存与高频数据承载层。

第一版建议：

- `cart`：Redis 优先，保留未来落 DB 的扩展点
- `product`：商品详情与价格缓存，采用 Cache Aside
- `auth`：Token 黑名单与登录态辅助缓存
- `recommendation`：热门商品、最近浏览结果缓存

设计原则：

- 先利用 Redis 提升热点读写能力
- 不在第一版引入复杂分布式一致性方案
- 下单时对购物车勾选项做订单快照，避免后续数据漂移

## 9. 安全模型

权限体系采用 RBAC。

系统角色：

- `CUSTOMER`
- `SALES`
- `ADMIN`

实现策略：

- 基于 Spring Security + JWT
- `auth` 模块负责登录认证、Token 生成与权限装配
- `backend-api` 负责安全过滤器和统一鉴权接入
- 使用角色到权限点映射方式管理接口访问
- 第一版不引入复杂数据权限模型

## 10. 可扩展性决策

为未来演进，首版骨架必须预留以下扩展点：

### 10.1 事件抽象

统一事件接口与发布器，保证后续从本地事件迁移到消息队列时，业务模块无需大改。

### 10.2 Outbox 预留

对订单和支付等关键模块预留 Outbox 结构与接口，以支持事务消息一致性扩展。

### 10.3 ID 生成策略

统一抽象 ID 生成策略，不把数据库自增主键作为系统边界 ID。

### 10.4 时间与用户上下文

统一抽象当前时间与当前用户上下文读取方式，便于测试、审计和未来扩展。

### 10.5 API 版本化

Controller 路由统一采用 `/api/v1` 前缀，避免后续版本演进冲突。

### 10.6 可观测性

首版骨架至少包含：

- 统一日志结构
- TraceId
- 健康检查
- 统一异常响应

后续可逐步扩展 metrics、tracing、审计日志。

## 11. 首版骨架范围

### 11.1 根聚合工程

- 根 POM
- 统一依赖管理
- 模块声明
- Java 与 Spring Boot 构建约束

### 11.2 后端入口应用

`apps/backend-api` 包含：

- Spring Boot 启动类
- Controller 组织骨架
- 统一响应结构
- 全局异常处理
- 基础健康检查
- `/api/v1` 路由规范

### 11.3 平台模块

生成全部平台模块基础代码与包结构。

### 11.4 业务模块

为 `user`、`auth`、`product`、`cart`、`order`、`analytics`、`recommendation` 生成基础包结构与骨架类。

### 11.5 最小闭环示例

`user` 与 `auth` 生成相对更完整的最小闭环，作为后续业务模块开发模板。

### 11.6 Flyway

生成首批数据库迁移脚本骨架与初始化表结构。

### 11.7 前端应用

生成 `apps/web` 与 `apps/admin` 的独立前端项目骨架与说明文件，不在本次实现复杂页面。

### 11.8 文档

补充以下文档：

- 模块依赖规则
- 新模块接入规范
- 事件命名规范
- 数据库迁移规范
- 本地启动说明

## 12. 实施说明

为了保证架构稳定，生成项目骨架时应遵守以下原则：

- 只在确有需要的地方提供实现，避免假完整
- 将边界和规则以代码结构体现，而不是只写在文档里
- 先建立稳定底座，再补业务细节
- 让未来接入 MQ、拆分服务、引入更强观测能力时不需要推翻当前结构

## 13. 风险与非目标

本次骨架不处理：

- 微服务拆分
- 分布式事务
- 复杂推荐算法
- 真实第三方支付网关
- 多商户能力

已知风险：

- 如果后续在开发中放松模块边界，严格模块化将失效
- 如果在 Controller 或平台模块中堆积业务逻辑，会导致演进成本快速升高
- 如果跳过 Flyway 管理数据库变更，后续环境一致性会被破坏

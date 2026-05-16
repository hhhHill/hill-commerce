# Implementation Plan: recommendation-engine

**Feature**: `recommendation-engine`  
**Status**: draft  

## Summary

接入 Gorse 开源推荐引擎，在首页和商品详情页底部自然混入推荐商品卡片。已登录用户展示个性化推荐，未登录用户展示热门/相似商品兜底。

本期不接 RocketMQ，也不引入消息队列。浏览和购买反馈先由后端直接 fire-and-forget 调用 Gorse，失败只记录 warn，不影响浏览、支付或页面主链路。后续如需消息队列，再单独创建对应 feature spec。

目标是先把"商品数据进入 Gorse → 行为反馈进入 Gorse → 推荐结果出来 → 前端自然展示"这条最小闭环跑通。

## Current Repository Reality

- `application.yml` 中已有 `hill.rocketmq.enabled` 预留开关，但本期不使用
- `docker-compose.yml` 已有 MySQL 和 Redis，无 Gorse 服务定义
- 后端已有 `LoggingService.recordProductView()` 写入 `product_view_logs`，可在日志写入成功后追加 Gorse view feedback
- 后端已有 `PaymentService.succeed()` 处理支付完成逻辑，可在支付成功后加载订单商品并追加 purchase feedback
- 后端已有 `modules/product/` 下的商品实体、mapper 和商品管理服务，推荐结果可 JOIN 拼装商品详情，商品变更可触发 Gorse item 同步
- 前端已有 `ProductCard` 组件（`features/storefront/catalog/product-card.tsx`），推荐区域可复用它
- 前端首页 `app/page.tsx` 和商品详情页 `app/products/[productId]/page.tsx` 均已存在，推荐区域为增量插入
- 前端已有 `lib/storefront/client.ts` 和 `types.ts`，可扩展推荐接口
- `specs/product-discovery` 已将"推荐系统正式版"列为 Out of Scope，本 feature 为其补充实现

## Technical Boundaries

- Gorse 作为独立 Docker 容器部署，不嵌入 Spring Boot 进程
- 推荐结果通过 Spring Boot 后端代理拉取，前端不直接访问 Gorse
- 浏览/购买反馈由后端直接调用 Gorse API；失败 silent fail + log warn
- 前端推荐区域复用 `product-discovery` 的商品卡片组件和展示规则
- Gorse 不可用时推荐接口返回空列表，前端隐藏推荐区域
- 不新增 RocketMQ 依赖、starter、consumer、producer 或 compose 服务
- 首期优先使用 Spring HTTP client 直接调用 Gorse REST API，避免 SDK 版本不稳定风险

## Planned Module Shape

### Backend New Modules

- `backend/src/main/java/com/hillcommerce/modules/recommendation/`
  - `GorseClient.java`：封装 Gorse REST API（插入/更新商品、插入反馈、拉取推荐结果）
  - `GorseCatalogSyncService.java`：商品同步、上下架状态同步、初始化 backfill
  - `GorseFeedbackService.java`：浏览和购买 feedback 的 fire-and-forget 写入
  - `RecommendationService.java`：推荐业务逻辑（判断登录/未登录、调用 Gorse、拼装商品详情、过滤不可售/已购买）
  - `web/StorefrontRecommendationController.java`：前台推荐接口
  - `web/RecommendationDtos.java`：推荐接口返回模型

### Backend Modified Files

- `modules/logging/service/LoggingService.java`：`recordProductView()` 追加 Gorse view feedback
- `modules/payment/service/PaymentService.java`：支付成功后按 `order_items` fan-out 发送 purchase feedback
- 商品管理相关 service：商品创建/更新/上下架/删除后同步 Gorse item
- `application.yml`：新增 `hill.recommendation.gorse.*` 配置

### Frontend New Files

- `frontend/next-app/src/lib/storefront/recommendation-client.ts`：推荐接口调用
- `frontend/next-app/src/lib/storefront/recommendation-types.ts`：推荐数据类型
- `frontend/next-app/src/features/storefront/catalog/recommendation-section.tsx`：推荐商品区域组件

### Frontend Modified Files

- `frontend/next-app/src/app/page.tsx`：首页混入推荐区域
- `frontend/next-app/src/app/products/[productId]/page.tsx`：详情页底部混入推荐区域

### Infrastructure

- `docker-compose.yml`：新增 `gorse` 服务
- `ops/gorse/config.toml`：Gorse 配置文件
- MySQL 初始化：确保存在 `gorse` database，并确保 Gorse 使用的数据库用户具备访问权限

## Implementation Slices

### 1. Gorse Infrastructure Slice

- 部署 Gorse：`gorse-in-one` 容器，配置文件挂载，连接 MySQL + Redis
- 新增后端 Gorse endpoint / enabled / timeout 配置
- 验证 Gorse dashboard 和 HTTP API 可访问

### 2. Catalog Sync Slice

- 实现 `GorseClient` 的 item 写入/更新
- 实现 `GorseCatalogSyncService`
- 商品创建/更新/上下架/删除后同步 Gorse
- 提供初始化 backfill 方法或管理端触发入口，将当前商品写入 Gorse

### 3. Feedback Slice

- 实现 `GorseFeedbackService`
- `LoggingService.recordProductView()` 写入日志后发送 view feedback
- `PaymentService.succeed()` 支付成功后加载订单商品，每个商品发送 purchase feedback
- Gorse 调用失败只 log warn，不抛出业务异常

### 4. Recommendation Read Slice

- 实现 `RecommendationService`
- 实现 `StorefrontRecommendationController`：`GET /api/storefront/recommendations`
- 首页未登录使用 `GET /api/popular`
- 首页已登录使用 `GET /api/recommend/{userId}`，不足时热门兜底
- 详情页未登录使用 `GET /api/item/{productId}/neighbors`
- 详情页已登录合并个性化推荐和 item neighbors
- 过滤下架/删除商品、当前商品、已购买商品

### 5. Frontend Slice

- 新增推荐类型定义和 API 调用
- 新增 `RecommendationSection` 组件
- 首页和详情页混入推荐区域
- 验证未登录/已登录两种推荐展示
- 验证 Gorse 不可用时推荐区域隐藏

## Execution Order

1. Gorse Infrastructure Slice
2. Catalog Sync Slice
3. Feedback Slice
4. Recommendation Read Slice
5. Frontend Slice

Catalog Sync 必须早于 Recommendation Read，否则 Gorse 可能没有 item 数据，热门、最新和相似推荐都不稳定。

## Verification Path

- `docker compose up -d mysql redis gorse` 启动依赖服务，确认 Gorse dashboard 和 health API 可访问
- 执行商品 backfill，确认 Gorse 中存在 item
- 未登录用户访问首页，确认展示热门商品卡片
- 登录用户浏览商品后访问首页，确认推荐区域展示；如果模型尚未训练，确认热门兜底生效
- 登录用户进入商品详情页，确认底部展示推荐商品且不包含当前商品
- 支付包含多个商品的订单，确认每个商品都有 purchase feedback 写入 Gorse
- 模拟 Gorse 挂掉，确认首页、详情页、浏览日志和支付成功主链路不受影响

## Risks

- Gorse 配置和 API 版本需要以目标镜像版本为准，配置字段必须在实现前验证
- Gorse 训练间隔 60 分钟，新用户浏览后短期内可能仍返回热门兜底，这是预期行为
- 如果未做商品 backfill 或商品状态同步，推荐结果可能为空或包含不可售商品
- 直接调用 Gorse 比消息队列更简单，但不具备削峰和重试能力；后续接入消息队列时应单独设计异步管道
- Gorse 与 MySQL 共享实例时需注意 database 隔离，建议独立 `gorse` database

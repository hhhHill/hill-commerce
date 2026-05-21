# Tasks: recommendation-engine

**Status**: draft

## Goal

完成 Gorse 推荐引擎第一阶段接入，不使用 RocketMQ 或其他消息队列。实现首页和商品详情页底部自然混入推荐商品卡片：已登录用户个性化推荐，未登录用户热门/相似推荐兜底。

## Implementation Order

### Phase 1: Gorse Infrastructure

- [X] 在 `docker-compose.yml` 中新增 `gorse` 服务定义（使用 `zhenghaoz/gorse-in-one` 镜像）
- [X] 新增 `ops/gorse/config.toml`，配置反馈类型、协同过滤参数、item neighbors、popular/latest fallback
- [ ] 确保 MySQL 中存在 `gorse` database，并确保 Gorse 使用的数据库用户具备访问权限
- [X] 在 `backend/src/main/resources/application.yml` 中新增 `hill.recommendation.gorse.enabled`、`endpoint`、`timeout` 配置
- [ ] 验证 `docker compose up -d mysql redis gorse` 后 Gorse dashboard 和 health API 可访问

### Phase 2: Gorse Client + Catalog Sync

- [X] 新增 `backend/src/main/java/com/hillcommerce/modules/recommendation/GorseClient.java`，封装 Gorse REST API：
  - `insertOrUpdateItem(Item item)`
  - `insertFeedback(Feedback feedback)`
  - `getRecommend(String userId, int n)`
  - `getPopular(int n)`
  - `getItemNeighbors(String itemId, int n)`
- [X] 新增 `backend/src/main/java/com/hillcommerce/modules/recommendation/GorseCatalogSyncService.java`
  - 商品创建/更新时写入 Gorse item
  - 商品上架时 `IsHidden=false`
  - 商品下架/删除时 `IsHidden=true` 或等价不可推荐状态
  - 初始化 backfill 当前商品数据
- [X] 修改商品管理相关 service，在商品创建、更新、上下架、删除后调用 `GorseCatalogSyncService`
- [ ] 编写 `GorseClient` 单元测试（Mock Gorse HTTP 响应）
- [X] 编写 `GorseCatalogSyncService` 单元测试（覆盖上架、下架、删除、backfill）

### Phase 3: Direct Feedback Write

- [X] 新增 `backend/src/main/java/com/hillcommerce/modules/recommendation/GorseFeedbackService.java`
  - `fireAndForgetView(userId, anonymousId, productId, timestamp)`
  - `fireAndForgetPurchase(userId, productId, timestamp)`
  - Gorse 调用失败时 `log.warn`，不抛业务异常
- [X] 修改 `LoggingService.recordProductView()`，在 product view log 写入成功后调用 `GorseFeedbackService.fireAndForgetView()`
- [X] 修改 `PaymentService.succeed()`，支付成功后读取该订单全部 `order_items`，每个商品调用一次 `fireAndForgetPurchase()`
- [X] 编写 `GorseFeedbackService` 单元测试（覆盖成功、失败吞异常）
- [ ] 编写 `PaymentService` 相关测试，验证多商品订单会 fan-out 多条 purchase feedback，且 Gorse 失败不影响支付成功

### Phase 4: Recommendation Read

- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/recommendation/web/RecommendationDtos.java`，定义推荐返回模型
- [X] 新增 `backend/src/main/java/com/hillcommerce/modules/recommendation/RecommendationService.java`：
  - 未登录首页 → 调用 `GorseClient.getPopular()`
  - 已登录首页 → 调用 `GorseClient.getRecommend(userId)`，不足时热门兜底
  - 未登录详情页 → 调用 `GorseClient.getItemNeighbors(productId)`
  - 已登录详情页 → 合并 `getRecommend(userId)` 与 `getItemNeighbors(productId)`
  - 根据返回 itemId 列表 JOIN 商品主数据，拼装商品卡片信息
  - 过滤已下架/不可售/已删除商品
  - 过滤当前商品
  - 去重并排除用户已购买商品
- [X] 新增 `backend/src/main/java/com/hillcommerce/modules/recommendation/web/StorefrontRecommendationController.java`：
  - `GET /api/storefront/recommendations?type=home&n=10`
  - `GET /api/storefront/recommendations?type=detail&productId={id}&n=6`
- [X] 推荐接口参数校验：
  - `type` 只允许 `home` / `detail`
  - `detail` 必须提供 `productId`
  - `n` 默认按场景取 10 或 6，最大 50
  - 接口公开访问，但登录态存在时使用认证用户 ID
- [X] 编写 `RecommendationService` 单元测试
- [ ] 编写 `StorefrontRecommendationController` 集成测试（覆盖未登录热门、已登录个性化、详情页相似、Gorse 不可用降级、参数校验）

### Phase 5: Frontend Integration

- [X] 新增 `frontend/next-app/src/lib/storefront/recommendation-types.ts`，定义推荐返回类型
- [X] 新增 `frontend/next-app/src/lib/storefront/recommendation-client.ts`，封装推荐接口调用
- [X] 新增 `frontend/next-app/src/features/storefront/catalog/recommendation-section.tsx`，推荐商品区域组件：
  - 复用 `ProductCard` 展示推荐商品
  - 无推荐结果时隐藏区域
  - 加载中展示骨架屏
- [X] 修改 `frontend/next-app/src/app/page.tsx`，首页混入推荐区域
- [X] 修改 `frontend/next-app/src/app/products/[productId]/page.tsx`，详情页底部混入推荐区域
- [ ] 手动验证：未登录用户首页展示热门商品
- [ ] 手动验证：已登录用户首页展示个性化推荐或热门兜底
- [ ] 手动验证：商品详情页底部展示推荐商品且不包含当前商品
- [ ] 手动验证：Gorse 不可用时推荐区域隐藏，首页和详情页正常浏览

### Phase 6: Resilience & Polish

- [X] Gorse 不可用时 `RecommendationService` 返回空列表，不抛异常
- [X] 浏览反馈写入 Gorse 失败时只 `log.warn`，不影响 `product_view_logs` 写入和页面浏览
- [X] 购买反馈写入 Gorse 失败时只 `log.warn`，不影响支付成功和订单状态更新
- [X] 前端推荐区域加载态与空态完善

## Dependencies

- Phase 1 完成后，Phase 2 才能验证 Gorse item 写入
- Phase 2 完成后，Phase 4 才有稳定 item 数据可推荐
- Phase 3 与 Phase 4 可以部分并行，但购买 fan-out 依赖订单商品查询
- Phase 4 完成后，Phase 5 依赖推荐接口和返回模型稳定
- Phase 6 可与 Phase 3/4/5 并行推进

## Suggested MVP Scope

- Phase 1（Gorse 基础设施）
- Phase 2（商品同步与 backfill）
- Phase 3（直接反馈写入）
- Phase 4（推荐读取）
- Phase 5（前端展示）
- Phase 6 中的降级处理（Gorse 不可用不报错）

不包含 RocketMQ、Broker、NameServer、消息生产者、消息消费者、消息重试和消息堆积治理。

## Done When

- `docker compose up -d mysql redis gorse` 能启动 Gorse，dashboard/API 可访问
- 商品 backfill 后，Gorse 中能看到当前可售商品
- 未登录用户首页展示热门商品卡片
- 已登录用户首页展示个性化推荐或热门兜底
- 商品详情页底部展示推荐商品，且不包含当前商品
- 推荐商品卡片与现有商品卡片样式一致，无推荐理由标注
- 用户浏览商品后，浏览 feedback 已推送至 Gorse（可通过 Gorse 仪表盘或 API 确认）
- 用户购买多商品订单后，每个商品都有 purchase feedback 推送至 Gorse
- Gorse 不可用时，推荐区域隐藏，首页、详情页、浏览日志和支付成功主链路正常
- 本 feature 未新增 RocketMQ 依赖、服务或运行时要求

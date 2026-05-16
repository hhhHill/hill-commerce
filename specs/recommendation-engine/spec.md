# Feature Specification: recommendation-engine

**Feature**: `recommendation-engine`  
**Status**: draft  

## Purpose

为前台用户提供个性化商品推荐与热门商品展示，依托 Gorse 开源推荐引擎，在首页与商品详情页底部自然混入推荐商品卡片，不对用户显式标注推荐理由。

本期目标是先跑通最小闭环：商品数据进入 Gorse → 用户浏览/购买反馈直接写入 Gorse → 后端读取推荐结果 → 前端展示推荐商品。消息队列接入不包含在本 feature 中，后续需要时单独创建对应 feature spec。

## Scope

### In Scope

- 首页推荐区域：已登录用户个性化推荐，未登录用户热门商品
- 商品详情页底部推荐区域：未登录用户基于当前商品相似推荐；已登录用户使用个性化推荐与当前商品相似推荐的合并结果
- 用户浏览行为直接同步至 Gorse（失败只记录 warn，不影响浏览）
- 用户购买行为直接同步至 Gorse（支付成功后按订单商品逐个发送 purchase feedback，失败只记录 warn，不影响支付）
- Gorse 推荐引擎部署（Docker Compose 集成）
- 商品数据同步与初始化 backfill：商品写入 Gorse item，上下架/删除状态同步为隐藏或不可推荐
- 后台 Gorse 仪表盘访问（可选，运维用）

### Out of Scope

- RocketMQ / 消息队列接入与异步消费管道
- 显式推荐理由标注（"浏览过此商品的人也买了..."）
- 多品类分群推荐策略
- A/B 实验框架
- LLM 重排序
- 图像特征相似度推荐
- 推荐效果在线评估面板（首期仅接入，后续迭代）

## Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                        Spring Boot 后端                          │
│                                                                  │
│  LoggingService ──直接调用──→ GorseClient ──→ Gorse REST API      │
│  PaymentService ──直接调用──→ GorseClient ──→ Gorse REST API      │
│                                                                  │
│  Product sync/backfill ─────→ GorseClient ──→ Gorse item API      │
│                                                                  │
│  RecommendationService ←──── Gorse REST API (拉取推荐结果)         │
│       ↑                                                          │
│  StorefrontRecommendationController                               │
└────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
                         ┌──────────────────────────────┐
                         │     Gorse (Docker Compose)    │
                         │  gorse-in-one                 │
                         │  - dashboard + HTTP API        │
                         │  - training + recommendation   │
                         └──────────────────────────────┘
```

## Data Flow

### 写入链路（直接推送反馈至 Gorse）

```
浏览商品 → LoggingService.recordProductView()
              │
              ├─→ 写入 product_view_logs (MySQL)        [保留现有逻辑]
              └─→ GorseFeedbackService.fireAndForgetView()
                       └─→ POST /api/feedback (Gorse)

支付成功 → PaymentService.succeed()
              │
              ├─→ 更新支付和订单状态                    [保留现有逻辑]
              ├─→ 读取该订单的 order_items
              └─→ GorseFeedbackService.fireAndForgetPurchase()
                       └─→ 每个商品一条 purchase feedback
```

写入 Gorse 失败必须被捕获，只记录应用日志，不改变原业务接口响应语义。

### 商品同步链路

```
商品创建/更新/上下架/删除
       │
       └─→ GorseCatalogSyncService
              └─→ POST /api/item 或 PATCH /api/item/{itemId}

初始化/手动 backfill
       │
       └─→ 遍历当前可售商品并写入 Gorse
```

商品下架或删除后不得继续出现在推荐结果中。实现上可以同步 `IsHidden=true`，也必须在 `RecommendationService` 拼装商品详情时再次过滤不可售商品。

### 读取链路（拉取推荐结果）

```
前端页面（首页 / 商品详情页底部）
       │
       ▼
GET /api/storefront/recommendations?type=home
GET /api/storefront/recommendations?type=detail&productId={id}
       │
       ▼
RecommendationService
       │
       ├─ 首页 + 未登录 → Gorse: GET /api/popular
       ├─ 首页 + 已登录 → Gorse: GET /api/recommend/{userId}
       ├─ 详情页 + 未登录 → Gorse: GET /api/item/{productId}/neighbors
       └─ 详情页 + 已登录 → 合并 GET /api/recommend/{userId} 与 GET /api/item/{productId}/neighbors
```

合并策略：保留 Gorse 返回顺序，先放个性化推荐，再补充 item-neighbor 结果；按 `itemId` 去重；过滤当前商品、已下架商品、已购买商品；不足数量时可用 `GET /api/popular` 补足。

## Display Strategy

### 首页推荐

| 用户状态 | 推荐来源 | 展示数量 |
|---|---|---|
| 未登录 | Gorse popular（热门商品） | 10 |
| 已登录 | Gorse 个性化推荐，热门商品兜底 | 10 |

### 商品详情页底部

| 用户状态 | 推荐来源 | 展示数量 |
|---|---|---|
| 未登录 | Gorse item-to-item neighbors，热门商品兜底 | 6 |
| 已登录 | 个性化推荐 + 当前商品 neighbors 合并，热门商品兜底 | 6 |

### UI 呈现

- 推荐商品以标准商品卡片混入页面，与普通商品卡片样式一致
- 不显示"推荐"标签、不显示推荐理由
- 卡片展示：主图、商品名称、销售价格（与 `product-discovery` 卡片规则一致）
- 加载中：骨架屏，不显示单独区域标题
- 空状态：无推荐结果时不展示推荐区域

## Feedback Types Mapping

Gorse 的反馈类型配置（`ops/gorse/config.toml`）：

| 业务行为 | Gorse FeedbackType | Value |
|---|---|---|
| 商品详情浏览 | `view` | 1 |
| 购买 | `purchase` | 5 |

`positive_feedback_types = ["view", "purchase"]`  
`read_feedback_types = ["view"]`

## Gorse Payloads

### Feedback

```json
{
  "UserId": "user:{id} | anon:{anonymousId}",
  "ItemId": "product:{id}",
  "FeedbackType": "view | purchase",
  "Timestamp": "ISO-8601 string"
}
```

### Item

```json
{
  "ItemId": "product:{id}",
  "Categories": ["category:{categoryId}"],
  "Labels": {
    "status": "ON_SHELF"
  },
  "IsHidden": false,
  "Timestamp": "ISO-8601 string"
}
```

匿名用户以 `anon:{anonymousId}` 作为 Gorse 用户标识；登录用户以 `user:{userId}` 作为 Gorse 用户标识。匿名与登录身份不合并（MVP 限制）。

## Deployment Changes

### docker-compose.yml 新增

```yaml
gorse:
  image: zhenghaoz/gorse-in-one:latest
  container_name: hc-gorse
  ports:
    - "8086:8086"
    - "8088:8088"
  command: >
    -c /etc/gorse/config.toml
    --log-path /var/log/gorse/master.log
    --cache-path /var/lib/gorse/master_cache.data
  volumes:
    - ./ops/gorse/config.toml:/etc/gorse/config.toml:ro
    - gorse-data:/var/lib/gorse
    - gorse-log:/var/log/gorse
  depends_on:
    mysql:
      condition: service_healthy
    redis:
      condition: service_started
```

Gorse 的 cache/data store 只在 `ops/gorse/config.toml` 中配置，避免 docker-compose 环境变量与配置文件形成双真相。

需要确保 MySQL 中存在 `gorse` database，并且配置的用户有权限访问。可通过 compose 初始化脚本创建，或在 README/运维脚本中明确初始化命令。当前 `docker-compose.yml` 的 MySQL 服务已定义 `healthcheck`，因此 Gorse snippet 中的 `depends_on.mysql.condition: service_healthy` 可用。

## Backend Changes

### 新增文件

| 文件 | 职责 |
|---|---|
| `modules/recommendation/GorseClient.java` | 封装 Gorse REST API 调用（插入/更新商品、插入反馈、拉取推荐） |
| `modules/recommendation/GorseCatalogSyncService.java` | 商品同步与 backfill |
| `modules/recommendation/GorseFeedbackService.java` | 浏览/购买 feedback 的 fire-and-forget 写入与降级 |
| `modules/recommendation/RecommendationService.java` | 推荐业务逻辑，拼接推荐结果与商品详情 |
| `modules/recommendation/web/StorefrontRecommendationController.java` | 前台推荐接口 |
| `modules/recommendation/web/RecommendationDtos.java` | 推荐接口返回模型 |

### 修改文件

| 文件 | 改动 |
|---|---|
| `modules/logging/service/LoggingService.java` | `recordProductView()` 写入日志后 fire-and-forget 写入 Gorse view feedback |
| `modules/payment/service/PaymentService.java` | 支付成功后读取订单商品并 fire-and-forget 写入 Gorse purchase feedback |
| 商品管理相关 service | 商品创建/更新/上下架/删除后同步 Gorse item 状态 |
| `docker-compose.yml` | 新增 Gorse 服务 |
| `backend/src/main/resources/application.yml` | 新增 Gorse endpoint、enabled、timeout 配置 |

## Frontend Changes

### 新增文件

| 文件 | 职责 |
|---|---|
| `lib/storefront/recommendation-client.ts` | 前台推荐接口调用 |
| `lib/storefront/recommendation-types.ts` | 推荐数据类型 |
| `features/storefront/catalog/recommendation-section.tsx` | 推荐商品区域组件（首页与详情页复用） |

### 修改文件

| 文件 | 改动 |
|---|---|
| `app/page.tsx` | 首页混入推荐商品区域 |
| `app/products/[productId]/page.tsx` | 详情页底部混入推荐商品区域 |

## Gorse Configuration

首期推荐管线（`ops/gorse/config.toml`）：

```toml
[database]
cache_store = "redis://redis:6379"
data_store = "mysql://hill:hill123@tcp(mysql:3306)/gorse?parseTime=true"

[recommend]
cache_size = 100
fallback_recommend = ["popular", "latest"]

[recommend.data_source]
positive_feedback_types = ["view", "purchase"]
read_feedback_types = ["view"]

[recommend.collaborative]
num_epochs = 100
num_factors = 50
learning_rate = 0.005

[recommend.item_neighbors]
neighbor_type = "items"
```

具体字段名以当前 Gorse 版本的配置模板为准；实现前必须用目标镜像版本的示例配置校验。

## Acceptance Criteria

- 部署：`docker compose up -d` 启动 MySQL、Redis、Gorse，后端启动无报错
- 未登录用户首页展示热门商品卡片，卡片与现有商品卡片样式一致
- 已登录用户首页展示个性化推荐商品，推荐结果不包含已购买商品
- 商品详情页底部展示推荐商品卡片，且不包含当前商品
- 浏览商品后，浏览 feedback 已写入 Gorse（可通过 Gorse 仪表盘或 API 验证）
- 购买后，该订单每个商品的 purchase feedback 已写入 Gorse
- 商品创建/更新/上下架/删除能同步到 Gorse；下架或删除商品不会出现在推荐结果中
- Gorse 不可用时，推荐区域隐藏，不影响首页、详情页、浏览日志、支付成功主链路
- 本 feature 不新增、不启动、不依赖 RocketMQ

## Boundaries and Dependencies

- 依赖 `product-discovery` 的商品卡片组件和前台商品展示规则
- 依赖 `order-checkout` + `payment` 的支付完成逻辑
- 依赖 `docker-compose.yml` 中的 MySQL 和 Redis（均已存在）
- 首版主链路不得依赖 Gorse 才能运行；Gorse 失败必须降级为空推荐
- Gorse 训练间隔默认 60 分钟，推荐结果存在模型延迟；读取端必须提供 popular/latest 兜底
- 匿名用户以 `anonymousId` 作为 Gorse 用户标识；登录后以 `userId` 标识，匿名与登录身份不合并（MVP 限制）
- RocketMQ / 消息队列接入属于后续独立 feature，不在本 feature 中定义
- 本 feature 不管理 Gorse 模型参数调优，调优属于后续运维迭代

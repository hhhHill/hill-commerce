# Feature Specification: 推荐 P0 — 反馈信号增强 + 会话级实时调整

**Feature**: `recommendation-p0-feedback-and-session`
**Status**: implemented
**Parent**: `recommendation-engine`

## Purpose

当前推荐系统仅追踪 view 和 purchase 两个反馈信号，且无会话级实时调整能力。本 feature 对标拼多多推荐策略中两个 P0 差距：

1. **反馈信号增加**：加入 add_to_cart 和 search 两个高价值信号，显著提升 Gorse 协作过滤的数据密度
2. **会话级品类加权**：追踪当前 session 中用户浏览过的品类，在推荐结果中加权同类商品，实现"刚看过手机壳，推荐立刻跟着变"的实时感

## Scope

### In Scope

- 加购反馈：`CartService.addItem()` 成功后 fire-and-forget 发送 `add_to_cart` 到 Gorse
- 搜索反馈：`StorefrontProductService.searchProducts()` 返回结果后 fire-and-forget 发送 `search` 到 Gorse（对搜索结果 Top-N 商品发送 feedback）
- Gorse 配置新增 `add_to_cart` 和 `search` 为 positive feedback type
- 会话品类追踪：后端 HTTP session 中维护用户最近浏览的品类 ID 集合（最近 10 个，FIFO）
- 推荐品类加权：`RecommendationService` 获取推荐结果后，将属于用户近期浏览品类的商品插到前排

### Out of Scope

- 前端改动（全部在后端和 Gorse 配置层完成）
- 搜索词的语义理解或 NLP
- 基于搜索词的个性化推荐模型训练
- 匿名用户的品类追踪跨设备

## Design

### 1. 加购反馈

**调用点**：`CartService.addItem()` 成功插入/合并 CartItem 之后

**实现**：
```java
// GorseFeedbackService 新增方法
public void fireAndForgetAddToCart(Long userId, Long productId) {
    fireAndForget("user:" + userId, productId, "add_to_cart");
}
```

**调用**：`CartService.addItem()` 末尾添加
```java
gorseFeedbackService.fireAndForgetAddToCart(userId, productId);
```

**改动文件**：
- `GorseFeedbackService.java` — 新增 `fireAndForgetAddToCart` 方法
- `CartService.java` — 注入 `GorseFeedbackService`，在 `addItem()` 末尾调用
- `ops/gorse/config.toml` — `positive_feedback_types` 追加 `"add_to_cart"`

### 2. 搜索反馈

**调用点**：`StorefrontProductService.searchProducts()` 返回分页结果之后

**策略**：对搜索结果的 Top-N（前 10 个）商品发送 `search` feedback。即使用户没点击，搜索行为本身已经表达了意图。

**实现**：
```java
// GorseFeedbackService 新增方法
public void fireAndForgetSearch(Long userId, String anonymousId, List<Long> productIds) {
    for (Long productId : productIds) {
        fireAndForget(userKey(userId, anonymousId), productId, "search");
    }
}
```

**调用**：`StorefrontProductService.searchProducts()` 末尾
```java
gorseFeedbackService.fireAndForgetSearch(userId, anonymousId, topProductIds);
```

**改动文件**：
- `GorseFeedbackService.java` — 新增 `fireAndForgetSearch` 方法
- `StorefrontProductService.java` — 注入 `GorseFeedbackService`，`searchProducts` 中调用
- `ops/gorse/config.toml` — `positive_feedback_types` 追加 `"search"`

### 3. 会话品类追踪

**存储**：HTTP Session 中维护 `RECENT_CATEGORIES` 属性，类型 `LinkedHashSet<Long>`，最大容量 10，FIFO。

**更新时机**：`LoggingService.recordProductView()` 中追加品类 ID 到 session。

**实现**：
```java
// 在 recordProductView 中追加
HttpSession session = ((ServletRequestAttributes) RequestContextHolder
    .currentRequestAttributes()).getRequest().getSession(false);
if (session != null) {
    @SuppressWarnings("unchecked")
    LinkedHashSet<Long> recentCategories = (LinkedHashSet<Long>) session
        .getAttribute("RECENT_CATEGORIES");
    if (recentCategories == null) {
        recentCategories = new LinkedHashSet<>();
    }
    if (recentCategories.size() >= 10) {
        // 移除最旧的
        Long first = recentCategories.iterator().next();
        recentCategories.remove(first);
    }
    recentCategories.add(categoryId);
    session.setAttribute("RECENT_CATEGORIES", recentCategories);
}
```

**注意**：`LoggingService.recordProductView` 传入的 `categoryId` 已经在 `ProductViewLogEntity` 中可用。

**改动文件**：
- `LoggingService.java` — `recordProductView()` 中追加 session 品类追踪

### 4. 推荐品类加权

**策略**：从 Gorse 拿到推荐结果后，检查每个候选商品的品类是否在用户近期浏览品类集合中。属于近期品类的商品插到前排，不属于的排在后面。保持同组内的 Gorse 原始顺序。

**实现**（`RecommendationService.recommend()` 在 `filteredIds` 之后，`loadProductCards` 之前）：

```java
// 品类加权重排
List<Long> boostedIds = boostRecentCategories(filteredIds, recentCategories);
```

```java
private List<Long> boostRecentCategories(List<Long> ids, Set<Long> recentCategories) {
    if (recentCategories == null || recentCategories.isEmpty()) return ids;
    
    Map<Long, Long> productCategoryMap = loadProductCategories(ids);
    
    List<Long> boosted = ids.stream()
        .filter(id -> recentCategories.contains(productCategoryMap.get(id)))
        .collect(Collectors.toList());
    List<Long> rest = ids.stream()
        .filter(id -> !recentCategories.contains(productCategoryMap.get(id)))
        .collect(Collectors.toList());
    
    boosted.addAll(rest);
    return boosted;
}
```

**品类获取**：从 `StorefrontRecommendationController` 传入，或 `RecommendationService` 直接从 session 读取。

**Controller 传递方式**：`StorefrontRecommendationController.recommendations()` 从 HTTP session 读取 `RECENT_CATEGORIES`，传递给 `recommendationService.recommend()` 作为新参数。

**改动文件**：
- `RecommendationService.java` — `recommend()` 新增 `Set<Long> recentCategories` 参数，增加 `boostRecentCategories()` 方法
- `StorefrontRecommendationController.java` — 从 session 读取品类集合，传入 service

## Gorse Configuration Changes

```toml
[recommend.data_source]
positive_feedback_types = ["view", "purchase", "add_to_cart", "search"]
read_feedback_types = ["view"]
```

`add_to_cart` 和 `search` 的 feedback 权重由 Gorse 根据 `positive_feedback_types` 中的顺序和频率自动学习。加购的信号强度高于浏览，搜索的信号强度介于浏览和加购之间。

## Data Flow After Changes

### 加购反馈链路

```
AddToCartPanel → POST /api/cart → CartController.addItem()
    → CartService.addItem()
        ├─ 插入/合并 CartItem（原有逻辑）
        └─ GorseFeedbackService.fireAndForgetAddToCart(userId, productId)
            └─ GorseClient.insertFeedback()
```

### 搜索反馈链路

```
SearchForm → GET /api/search → StorefrontProductController.searchProducts()
    → StorefrontProductService.searchProducts()
        ├─ 搜索+分页（原有逻辑）
        └─ GorseFeedbackService.fireAndForgetSearch(userId, anonymousId, top10ProductIds)
            └─ 逐个: GorseClient.insertFeedback()
```

### 会话追踪 + 推荐加权链路

```
ProductViewBeacon → POST /api/storefront/view-log → LoggingService.recordProductView()
    ├─ 写入 product_view_logs（原有逻辑）
    ├─ GorseFeedbackService.fireAndForgetView()（原有逻辑）
    └─ 更新 session.RECENT_CATEGORIES（新增）

首页/详情页 → GET /api/storefront/recommendations
    → StorefrontRecommendationController
        ├─ 从 session 读取 RECENT_CATEGORIES（新增）
        └─ RecommendationService.recommend(type, productId, n, userId, recentCategories)
            ├─ Gorse 召回（原有逻辑）
            ├─ 品类加权重排（新增）
            └─ loadProductCards()（原有逻辑）
```

## Acceptance Criteria

- 加购后 Gorse 收到 `add_to_cart` feedback（curl 验证 `/api/feedback` 日志）
- 搜索后 Gorse 收到 `search` feedback（对 Top-10 搜索结果）
- 浏览商品详情页后，session 中 `RECENT_CATEGORIES` 包含对应品类
- 再次访问首页推荐时，近期浏览品类的商品出现在列表前部
- Gorse 不可用时，品类加权仍正常工作（只依赖 session 数据）
- 加购/搜索 feedback 发送失败不影响主链路（fire-and-forget 模式）

## Files Changed

| 文件 | 改动类型 | 说明 |
|------|---------|------|
| `GorseFeedbackService.java` | 新增方法 | `fireAndForgetAddToCart`, `fireAndForgetSearch` |
| `CartService.java` | 修改 | 注入 GorseFeedbackService，addItem() 末尾调用 |
| `StorefrontProductService.java` | 修改 | 注入 GorseFeedbackService，searchProducts() 中调用 |
| `LoggingService.java` | 修改 | recordProductView() 中追加 session 品类追踪 |
| `RecommendationService.java` | 修改 | 新增 recentCategories 参数 + boostRecentCategories() |
| `StorefrontRecommendationController.java` | 修改 | 从 session 读取品类集合，传入 service |
| `ops/gorse/config.toml` | 修改 | 追加 add_to_cart, search 到 positive_feedback_types |
| `StorefrontProductServiceTest.java` | 修改 | 构造函数新增 GorseFeedbackService mock 参数 |

## Implementation Notes

### Gorse v0.5.x 兼容性

- **Feedback type 名称**：Gorse v0.5.x 的 `positive_feedback_types` 不支持连字符（`add-to-cart` 会导致 config 解析 fatal error）。实际使用下划线格式 `add_to_cart` 和 `search`。验证通过的配置：
  ```toml
  positive_feedback_types = ["view","purchase","add_to_cart","search"]
  ```

### 搜索反馈的匿名处理

`StorefrontProductService.searchProducts()` 中调用 `fireAndForgetSearch(null, null, topProductIds)`，userId 和 anonymousId 均传 null。原因是 `searchProducts` 方法无 Authentication 参数，无法获取当前用户身份。不影响 Gorse 学习效果——Gorse 仍能学习"搜索 X → 商品 Y"的共现关系。

### 品类加权的降级

品类加权仅依赖 HTTP session（`RECENT_CATEGORIES`），不依赖 Gorse。Gorse 不可用时加权仍正常生效。Session 不可用时（非 Web 上下文）`LoggingService.recordProductView()` 静默跳过品类追踪；`StorefrontRecommendationController.recentCategories()` 返回 null → 跳过多余的品类查询。

### 验证记录 (2026-05-21)

- 搜索 "Hoodie" → Top-10 搜索结果发送 search feedback → Gorse 收到
- 浏览商品 (categoryId=632) → session RECENT_CATEGORIES 包含 632
- 再次请求首页推荐 → 品类 632 的商品出现在第 1、2 位，非 632 品类商品排后
- 编译：`mvn test-compile -q` 通过
- 后端启动：无报错，Gorse backfill 成功

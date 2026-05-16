# 数据分析系统实施计划

**Feature**: `admin-analytics`
**Status**: active

## 总体思路

在现有 `AdminDashboardService` 基础上扩展，新增专职服务处理销售趋势、异常检测、用户画像和商品排行。采用预聚合表 + 定时任务的混合方案：高频数据走聚合表，按需查询走原始表。后端遵循 `JdbcTemplate` + Java record DTO 现有模式，前端遵循 server component 数据获取 + client component 渲染模式。

## 技术决策

### 聚合表查询

新建 3 张聚合表的实体和 Mapper 使用 MyBatis-Plus（与项目一致），定时任务的聚合写入使用 `JdbcTemplate` 跑 INSERT ... SELECT（与 `AdminDashboardService` 模式一致）。

### 定时任务

采用 Spring `@Scheduled`，与 `PaymentCloseService` 模式一致。通过配置属性 `hill.analytics.snapshot.cron` 和 `hill.analytics.daily-summary.cron` 可调节，默认值为 `0 5 * * * *` 和 `0 30 0 * * *`。需在 Spring Boot 主类或配置类上加 `@EnableScheduling`（检查是否已存在）。

### 异常检测

异常告警存于内存（ConcurrentHashMap），不持久化到数据库。服务重启后告警清空，下一轮检测时重新判定。轻量轮询端点 `/anomalies/status` 只返回 `{hasAlert, count}`，不查数据库。

### 用户画像

个体画像查询直接跑 SQL 聚合（不建用户缓存表），参数化查询避免 SQL 注入。购买力阈值硬编码为常量 `PURCHASING_POWER_THRESHOLDS`，可后续配置化。

### SALES 数据隔离

所有需要隔离的接口通过 `order_status_histories.changed_by` 关联，SALES 只看到自己操作过的订单产生的销售数据。隔离逻辑封装在 `SalesTrendService` 内部，controller 传入当前用户 ID 和角色。

### 前端图表库

使用 Recharts（`recharts` npm 包），体积小、声明式 React API。新增依赖：`npm install recharts`。

### 数据库变更

- V7 迁移：创建 `daily_sales_summary`、`product_sales_stats`、`hourly_sales_snapshot` 三张表

### 前端导航

在 `AdminShell` 的 `NAV_ITEMS` 中新增"数据分析"菜单项，指向 `/admin/analytics/overview`。

## 后端设计

### 新增文件清单

| 文件 | 职责 |
|------|------|
| `modules/admin/entity/DailySalesSummaryEntity.java` | MyBatis-Plus 实体 |
| `modules/admin/entity/ProductSalesStatsEntity.java` | MyBatis-Plus 实体 |
| `modules/admin/entity/HourlySalesSnapshotEntity.java` | MyBatis-Plus 实体 |
| `modules/admin/mapper/DailySalesSummaryMapper.java` | MyBatis-Plus BaseMapper |
| `modules/admin/mapper/ProductSalesStatsMapper.java` | MyBatis-Plus BaseMapper |
| `modules/admin/mapper/HourlySalesSnapshotMapper.java` | MyBatis-Plus BaseMapper |
| `modules/admin/service/SalesTrendService.java` | 趋势计算、环比/同比、移动平均 |
| `modules/admin/service/AnomalyDetectionService.java` | 异常检测、告警管理 |
| `modules/admin/service/UserProfileService.java` | 群体画像 + 个体画像 |
| `modules/admin/service/ProductRankingService.java` | 商品销量排行 |
| `modules/admin/service/AdminAnalyticsScheduler.java` | 定时聚合任务 |
| `modules/admin/web/AdminAnalyticsController.java` | REST 控制器 |
| `modules/admin/web/AdminAnalyticsDtos.java` | 请求/响应 DTO record |
| `resources/db/migration/V7__analytics_aggregation.sql` | 建表 DDL |

### 修改文件清单

| 文件 | 变更内容 |
|------|---------|
| `HillCommerceApplication.java` 或配置类 | 检查并添加 `@EnableScheduling` |
| `application.yml` | 添加 `hill.analytics.*` 配置项 |

### 数据流

```
AdminAnalyticsScheduler (每小时 H+5min / 每天 00:30)
  → JdbcTemplate INSERT ... SELECT
  → daily_sales_summary / product_sales_stats / hourly_sales_snapshot

GET /api/admin/analytics/trends?granularity=day&from=...&to=...
  → SalesTrendService.getTrends(granularity, from, to, userId, role)
    → 读 daily_sales_summary (按日期聚合)
    → 计算 SMA、环比、趋势方向
    → 返回 TrendResponse

GET /api/admin/analytics/anomalies/status
  → AnomalyDetectionService.getStatus()
    → 检查内存 ConcurrentHashMap
    → 返回 {hasAlert: boolean, count: int}

GET /api/admin/analytics/rankings/products?range=week&limit=10
  → ProductRankingService.getProductRankings(range, limit, userId, role)
    → 读 product_sales_stats (按范围过滤)
    → SALES 角色通过 order_status_histories 过滤
    → 返回 ProductRankingResponse

GET /api/admin/analytics/profiles/aggregate
  → UserProfileService.getAggregateProfiles()
    → 地域分布：读 user_addresses / orders 地址快照
    → 购买力分层：读 orders.payable_amount 按用户聚合分档
    → 偏好品类：读 order_items + products 按品类聚合
    → 返回 AggregateProfileResponse

GET /api/admin/analytics/profiles/users/search?keyword=xxx
  → UserProfileService.searchUsers(keyword)
    → LIKE 查询 users 表 (email / nickname)
    → 返回 List<UserProfileSummary>

GET /api/admin/analytics/profiles/users/{userId}
  → UserProfileService.getUserProfile(userId)
    → 聚合该用户的累计消费、品类偏好、地域、近90天订单数
    → 返回 UserProfileDetail
```

## 前端设计

### 新增文件清单

| 文件 | 职责 |
|------|------|
| `src/lib/admin/analytics-types.ts` | 分析相关 TypeScript 类型 |
| `src/lib/admin/analytics-client.ts` | 分析 API 客户端函数 |
| `src/features/admin/analytics/analytics-shell.tsx` | 子路由 tab 切换 |
| `src/features/admin/analytics/overview/kpi-cards.tsx` | KPI 卡片组件 |
| `src/features/admin/analytics/overview/anomaly-banner.tsx` | 异常告警横幅 |
| `src/features/admin/analytics/overview/sales-trend-chart.tsx` | 趋势图 (Recharts) |
| `src/features/admin/analytics/overview/overview-grid.tsx` | 概览页整合布局 |
| `src/features/admin/analytics/users/aggregate-panels.tsx` | 群体画像面板 |
| `src/features/admin/analytics/users/user-search-bar.tsx` | 用户搜索栏 |
| `src/features/admin/analytics/users/user-profile-detail.tsx` | 个体画像详情 |
| `src/features/admin/analytics/products/product-ranking-table.tsx` | 商品排行表格 |
| `src/features/admin/analytics/products/category-pie-chart.tsx` | 品类占比饼图 |
| `src/app/admin/analytics/overview/page.tsx` | 概览页 (server) |
| `src/app/admin/analytics/users/page.tsx` | 用户画像页 (server) |
| `src/app/admin/analytics/products/page.tsx` | 商品分析页 (server) |

### 修改文件清单

| 文件 | 变更内容 |
|------|---------|
| `src/features/admin/catalog/admin-shell.tsx` | `NAV_ITEMS` 新增"数据分析" |
| `src/lib/admin/types.ts` | 新增分析相关类型 |
| `src/lib/admin/client.ts` | 新增分析 API 函数 |
| `package.json` | 新增 `recharts` 依赖 |

### 组件树

```
/app/admin/analytics/overview/page.tsx          (server, 获取概览数据)
  └─ AdminShell
       └─ AnalyticsShell (tab: 概览 | 用户画像 | 商品分析)
            └─ OverviewGrid
                 ├─ AnomalyBanner (有告警时展示)
                 ├─ KpiCards × 4
                 ├─ SalesTrendChart (Recharts LineChart)
                 │    ├─ 日/周/月 Tab 切换
                 │    └─ 趋势线 + 环比标记
                 ├─ ProductRankingTable (Top 10, 迷你版)
                 └─ CategoryPieChart (品类占比)

/app/admin/analytics/users/page.tsx              (server, 获取群体画像)
  └─ AdminShell
       └─ AnalyticsShell
            ├─ UserSearchBar
            │    └─ 搜索结果下拉
            ├─ AggregatePanels (无搜索时展示)
            │    ├─ 地域分布柱状图
            │    ├─ 购买力分层饼图
            │    └─ 偏好品类排行
            └─ UserProfileDetail (选中用户后展示)

/app/admin/analytics/products/page.tsx           (server)
  └─ AdminShell
       └─ AnalyticsShell
            ├─ 日/周/月 Tab 切换
            ├─ ProductRankingTable (完整版, 分页)
            └─ CategoryPieChart
```

## 测试策略

### 后端集成测试

新建 `AdminAnalyticsIntegrationTest`，覆盖以下场景：

| 测试用例 | 验证点 |
|---------|--------|
| 小时快照写入 | `hourly_sales_snapshot` 表有数据写入 |
| 日汇总写入 | `daily_sales_summary` 正确聚合昨日订单 |
| 趋势接口返回 | `GET /trends?granularity=day` 返回正确数据结构 |
| 异常检测计算 | μ±2σ 判定逻辑正确，内存状态正确 |
| 告警确认 | 确认后内存移除 |
| 商品排行 | 日/周/月范围正确，Top N 正确 |
| 群体画像 | 地域/购买力/品类数据正确 |
| 个体画像 | 单用户查询返回完整数据 |
| SALES 隔离 | SALES 角色只能看到自己相关数据 |
| ADMIN 全量 | ADMIN 角色看到全局数据 |

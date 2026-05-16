# 代码审查记录：admin-analytics

**日期**: 2026-05-17
**状态**: 待修复

---

## Important

### 1. KPI "估算客单价"计算有误

**文件**: `frontend/next-app/src/features/admin/analytics/overview/kpi-cards.tsx:11`

```typescript
const average = latest ? amount / Math.max(1, latest.lastPeriodAmount ? 2 : 1) : 0;
```

- `lastPeriodAmount` 是数值却被当布尔值用，`amount / 2` 或 `amount / 1` 无业务含义
- `daily_sales_summary` 已有 `avg_order_amount` 字段，但趋势接口未返回

**方案**: 去掉"估算客单价"卡片，5 张卡片缩减为 4 张（当前销售额、上一周期销售额、趋势方向、环比变化）

---

### 2. 概览页与商品分析页内容重复

**文件**: `frontend/next-app/src/features/admin/analytics/overview/overview-grid.tsx`

概览页直接放了完整 `ProductRankingTable` + `CategoryPieChart`，与 `/admin/analytics/products` 相同。

**方案**: A — 概览页改为迷你 Top 10 列表（`<ol>` 紧凑列表，一行一个商品名+销量），不放饼图；完整表格和饼图只保留在 products 页

---

## Minor

### 3. 集成测试缺少业务断言

**文件**: `backend/src/test/java/com/hillcommerce/admin/AdminAnalyticsIntegrationTest.java`

只有 `contextLoads()`，未验证任何 API 行为。趋势/排行/画像服务缺集成测试。

**方案**: 后续迭代时补 `@Test` 方法，至少覆盖：趋势接口返回结构、异常检测判定、排行榜数据正确性、SALES 数据隔离

---

### 4. AnomalyBanner 缺少可点击入口

**文件**: `frontend/next-app/src/features/admin/analytics/overview/anomaly-banner.tsx:33`

文案写"请进入异常列表核查"但没有链接或可展开区域。

**方案**: 去掉这行文案，改为无操作提示："检测到 N 条销售异常告警"（不加行动号召）

---

### 5. `lastPeriodAmount` 语义偏差

**文件**: `backend/src/main/java/com/hillcommerce/modules/admin/service/SalesTrendService.java:95`

`lastPeriodAmount` 设为前一个点的 `amount`，对日粒度是"昨日"而非 spec 要求的"上周同期"。当前前端未使用此字段。

**方案**: 后续迭代修正为同环比语义（日粒度 = 昨日 vs 前日，周粒度 = 本周 vs 上周，月粒度 = 本月 vs 上月）

# Feature Specification: admin-analytics

**Feature**: `admin-analytics`
**Status**: active

## Purpose

为管理后台构建数据分析与监控系统，覆盖销售趋势、异常告警、商品排行、用户画像四大板块。在 `admin-dashboard` 的静态概览卡片之上，补齐时间序列图表、统计异常检测、用户画像查询和商品销售排行能力。

## Scope

### In Scope

- 销售趋势图（日/周/月粒度），含环比/同比和简单移动平均线
- 小时级销售快照 + 统计异常检测（μ ± 2σ），管理后台轮询告警
- 商品销量排行榜（当日/本周/本月），含品类占比饼图
- 用户画像：群体聚合（地域分布、购买力分层、偏好品类）+ 个体画像（按用户查询）
- 预聚合表 `daily_sales_summary`、`product_sales_stats`、`hourly_sales_snapshot` + `@Scheduled` 定时任务
- ADMIN 看全局，SALES 只看自己关联的订单数据

### Out of Scope

- 机器学习预测模型（使用统计趋势线代替）
- 推荐系统（后续接入 Gorse）
- 真实实时推送（使用前端 30s 轮询代替 WebSocket）
- 图表/数据导出（CSV/PDF）
- CUSTOMER 角色对分析数据的访问

## Data Model

### 新增表

**daily_sales_summary** — 日销售汇总

| 列 | 类型 | 约束 |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| stat_date | DATE | NOT NULL, UNIQUE |
| total_orders | INT | NOT NULL, DEFAULT 0 |
| total_amount | DECIMAL(18,2) | NOT NULL, DEFAULT 0 |
| paid_orders | INT | NOT NULL, DEFAULT 0 |
| cancelled_orders | INT | NOT NULL, DEFAULT 0 |
| avg_order_amount | DECIMAL(18,2) | NOT NULL, DEFAULT 0 |
| created_at | DATETIME(3) | NOT NULL, DEFAULT CURRENT_TIMESTAMP(3) |

**product_sales_stats** — 商品销售统计

| 列 | 类型 | 约束 |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| product_id | BIGINT | NOT NULL |
| product_name | VARCHAR(255) | NOT NULL |
| category_id | BIGINT | NOT NULL |
| total_quantity | INT | NOT NULL, DEFAULT 0 |
| total_amount | DECIMAL(18,2) | NOT NULL, DEFAULT 0 |
| stat_date | DATE | NOT NULL |
| created_at | DATETIME(3) | NOT NULL, DEFAULT CURRENT_TIMESTAMP(3) |
| UNIQUE(product_id, stat_date) | | |

**hourly_sales_snapshot** — 小时快照

| 列 | 类型 | 约束 |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| snapshot_hour | DATETIME(3) | NOT NULL |
| order_count | INT | NOT NULL, DEFAULT 0 |
| total_amount | DECIMAL(18,2) | NOT NULL, DEFAULT 0 |
| created_at | DATETIME(3) | NOT NULL, DEFAULT CURRENT_TIMESTAMP(3) |
| INDEX(snapshot_hour) | | |

## Business Rules

### 聚合任务

- 每小时 H+5min 写入 `hourly_sales_snapshot`（从 `payments` 表聚合已支付金额）
- 每天 00:30 写入 `daily_sales_summary` 和 `product_sales_stats`
- 聚合逻辑使用 `JdbcTemplate` 执行 INSERT ... SELECT，遵循项目已有模式

### 异常检测

- 取近 30 天对应小时段的 `hourly_sales_snapshot.total_amount` 序列
- 计算均值 μ 和标准差 σ
- 当前小时值 < μ - 2σ → 标记"异常偏低"；> μ + 2σ → 标记"异常偏高"
- 异常记录存于内存（ConcurrentHashMap），服务重启后重新判定
- 管理员通过 `POST /anomalies/{id}/acknowledge` 确认后移除
- 前端概览页每 30 秒轮询 `GET /anomalies/status`

### 趋势计算

- 日/周/月粒度分别从 `daily_sales_summary` 聚合
- 趋势线：近 7 日简单移动平均（SMA）
- 环比：与上一周期对比（日环比 = 昨日同时段，周环比 = 上周同日）
- 趋势方向：对近 7 个数据点做线性回归，斜率正 → "上升"，负 → "下降"

### 商品排行

- 支持当日/本周/本月切换，数据来自 `product_sales_stats`
- 默认 Top 10，可通过 `limit` 参数调整（最大 50）
- SALES 角色只看到自己发货的商品（通过 `order_status_histories.changed_by` 关联过滤）

### 用户画像

**购买力分层**（基于 `orders.payable_amount` 累计消费）：

| 层级 | 阈值 |
|------|------|
| 低 | 累计消费 < ¥500 |
| 中 | ¥500 ≤ 累计消费 < ¥5,000 |
| 高 | 累计消费 ≥ ¥5,000 |

**偏好分类**：按用户已支付订单的商品所属品类统计，取 Top 3

**地域**：优先取 `user_addresses` 默认地址的省/市，无地址时取订单快照地址

**个体画像展示**：昵称、邮箱、地域、累计消费金额、购买力层级、偏好品类 Top 3、近 90 天订单数

### SALES 数据隔离

- SALES 角色的趋势/排行接口，通过 `order_status_histories.changed_by = 当前用户ID` 且 `to_status = 'SHIPPED'` 过滤订单范围
- SALES 不可访问群体画像接口（`/profiles/aggregate`）
- SALES 可查看个体的基本信息但不可搜索所有用户

## API Boundaries

### 新增端点 — AdminAnalyticsController

前缀 `/api/admin/analytics`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/trends` | 销售趋势，参数 `granularity`: day/week/month，`from`，`to` 可选 | ADMIN, SALES |
| GET | `/anomalies` | 当前未消除的异常告警列表 | ADMIN, SALES |
| GET | `/anomalies/status` | 轻量轮询，返回 `{hasAlert, count}` | ADMIN, SALES |
| POST | `/anomalies/{id}/acknowledge` | 确认单条告警 | ADMIN |
| GET | `/rankings/products` | 商品销量排行，参数 `range`: today/week/month，`limit` 默认 10 | ADMIN, SALES |
| GET | `/profiles/aggregate` | 群体画像：地域分布、购买力分层、品类偏好 | ADMIN |
| GET | `/profiles/users/search` | 用户搜索，参数 `keyword` | ADMIN |
| GET | `/profiles/users/{userId}` | 个体画像详情 | ADMIN, SALES |

### 扩展接口

| 方法 | 路径 | 变更 |
|------|------|------|
| GET | `/api/admin/dashboard/summary` | 保留不变 |

## User Journeys

### Journey 1: 管理员查看销售趋势

管理员登录后台 → 侧边栏点击"数据分析" → 进入 `/admin/analytics/overview` → 看到今日/本周 KPI 卡片（订单数、销售额、客单价、环比变化）→ 下方为销售趋势折线图，默认按日展示 → 点击"周"或"月"切换粒度 → 图表联动更新，展示趋势线和环比标记。

### Journey 2: 异常告警触发与确认

系统每小时写入快照 → 异常检测逻辑判断当前小时销售额偏离超过 2σ → 标记告警 → 管理员在概览页看到顶部红色横幅"近 1 小时销售额异常偏低 -32%"→ 管理员评估后点击"确认"消除告警。

### Journey 3: 商品排行榜查看

管理员在概览页下方查看"商品销量 Top 10"→ 右侧饼图展示品类占比 → 点击"商品分析"子菜单进入 `/admin/analytics/products` → 查看完整排行表格，切换日/周/月范围。

### Journey 4: 用户画像查询

管理员进入 `/admin/analytics/users` → 上方展示群体画像（地域分布柱状图、购买力饼图、偏好品类排行）→ 在搜索框输入用户邮箱或昵称 → 下拉展示匹配用户 → 点击某个用户 → 右侧/下方展开个体画像面板（昵称、地域、累计消费、购买力层级、偏好品类、近 90 天订单数）。

### Journey 5: SALES 查看自己的数据

SALES 登录后台 → 进入数据分析 → 概览页 KPI 卡片和趋势图只展示自己发货的订单数据 → 商品排行只展示自己发货的商品 → 异常告警也只基于自己的数据范围判定 → 点击"用户画像"可查看个体用户基本信息，但不可搜索和查看群体画像。

## Pages

| 页面 | 路径 | 角色 |
|------|------|------|
| 销售概览 | `/admin/analytics/overview` | ADMIN, SALES |
| 用户画像 | `/admin/analytics/users` | ADMIN（SALES 可查看个体但无群体画像） |
| 商品分析 | `/admin/analytics/products` | ADMIN, SALES |

## Acceptance Criteria

- 管理员可在概览页查看日/周/月销售趋势折线图，含趋势线和环比变化
- 小时级异常检测能正确计算 μ±2σ 并生成告警
- 概览页每 30 秒轮询告警状态，有新告警时顶部展示红色横幅
- 管理员可确认消除告警
- 商品排行榜支持日/周/月切换，展示 Top 10 商品和品类占比饼图
- 群体画像正确展示地域分布、购买力分层、偏好品类
- 个体画像可通过搜索定位用户，展示累计消费、购买力层级、偏好品类
- SALES 只能看到自己发货相关数据，不可访问群体画像和用户搜索
- 定时任务在未启用 Redis / RocketMQ 条件下稳定运行
- `daily_sales_summary` 和 `product_sales_stats` 每天凌晨自动更新
- `hourly_sales_snapshot` 每小时自动更新

## Boundaries And Dependencies

- 订单和支付数据源由 `order-checkout`、`order-center`、`payment` 提供
- 商品和品类信息由 `admin-product-management` 和 `product-discovery` 提供
- 用户地址数据由 `auth-permission` 和 `admin-account-management` 提供
- 登录态和角色权限由 `auth-permission` 提供
- 管理后台框架和导航由现有 admin shell 组件提供
- 预聚合表为后续 Gorse 推荐系统接入提供数据基础

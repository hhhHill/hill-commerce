# Feature Specification: homepage-management

**Feature**: `homepage-management`
**Status**: active

## Purpose

将首页 2×2 活动卡片区域从硬编码常量改为后台可配置，Admin 可通过后台管理页面上传卡片图片、修改标题和跳转链接。同时数据模型预留 `placement` 字段，后续其他页面（分类页、活动页）可复用同一套卡片管理能力。

## Scope

### In Scope

- Admin 后台新增"首页运营"侧边栏入口和 `/admin/homepage` 管理页面
- 管理页面展示 4 张活动卡片表单：标题、图片上传（OSS）、跳转链接、启用开关
- 整批提交保存，不逐条 CRUD
- 前台首页从 API 读取活动卡片配置，替换硬编码 `ACTIVITY_CARDS`
- 新增 `activity_cards` 数据库表

### Out of Scope

- 首页其他区域（分类目录、用户面板、推荐商品列表）的管理
- 其他页面的卡片投放（仅模型预留 `placement` 字段，不做对应 admin 界面）
- 活动卡片样式变更
- 商品专区/精选推荐
- 卡片拖拽排序

## Data Model

### `activity_cards`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK AUTO_INCREMENT | 自增主键 |
| `placement` | VARCHAR(50) NOT NULL | 投放位置标识，如 `homepage` |
| `position` | INT NOT NULL | 卡位序号，同 placement 内从 0 开始 |
| `title` | VARCHAR(100) NOT NULL | 卡片标题 |
| `image_url` | VARCHAR(500) | OSS 图片地址，空则降级显示纯文字 |
| `link_url` | VARCHAR(500) NOT NULL | 点击跳转链接 |
| `is_active` | BOOLEAN NOT NULL DEFAULT TRUE | 是否启用 |
| `sort_order` | INT NOT NULL DEFAULT 0 | 排序权重 |
| `created_at` | DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP | |
| `updated_at` | DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | |

唯一约束：`UNIQUE(placement, position)`

### 初始化数据

```sql
INSERT INTO activity_cards (placement, position, title, link_url, is_active, sort_order) VALUES
('homepage', 0, '限时秒杀', '/search?keyword=限时秒杀', TRUE, 0),
('homepage', 1, '百亿补贴', '/search?keyword=百亿补贴', TRUE, 1),
('homepage', 2, '官方好货', '/search?keyword=官方好货', TRUE, 2),
('homepage', 3, '新品首发', '/search?keyword=新品首发', TRUE, 3);
```

## API

| 方法 | 路径 | 角色 | 说明 |
|------|------|------|------|
| `GET` | `/api/admin/activity-cards?placement=homepage` | ADMIN, MERCHANT | 查询某 placement 所有卡片 |
| `PUT` | `/api/admin/activity-cards` | ADMIN, MERCHANT | 批量更新卡片（body 为数组） |
| `GET` | `/api/storefront/activity-cards?placement=homepage` | 公开 | 获取已启用卡片 |

Admin 端不提供单条 CRUD，表内已有初始化数据，Admin 只做修改。

## Admin Page

- 路由：`/admin/homepage`
- 侧边栏："首页运营"，ADMIN 和 MERCHANT 均可见
- 页面内容：4 张卡片表单（标题 input + 图片上传 + 链接 input + 启用 switch），底部"保存"按钮
- 批量 PUT 提交，保存成功后 toast 提示
- 图片上传复用现有 OSS 上传组件

## Frontend Changes

- `app/page.tsx`：删除 `ACTIVITY_CARDS` 常量，改为从 `GET /api/storefront/activity-cards?placement=homepage` 获取
- 卡片渲染：有 `image_url` 时显示图片，无图片时降级显示纯文字（保持现有样式）
- API 失败或卡片全部为空时：降级显示空状态，不报错

## Acceptance Criteria

- Admin 侧边栏出现"首页运营"入口（ADMIN 和 MERCHANT 可见）
- `/admin/homepage` 页面可编辑 4 张活动卡片的标题、图片、链接、启用状态
- 保存后前台首页立即反映变更
- 前台首页 API 仅返回 `is_active = TRUE` 的卡片
- 卡片未上传图片时降级显示纯文字
- 数据库 `activity_cards` 表有初始化迁移脚本，部署即有四张默认卡片
- TypeScript 编译零错误

## Boundaries and Dependencies

- 图片上传依赖现有 OSS 模块（`/api/admin/oss/upload`）
- Admin 页面依赖现有 admin layout 和 AdminSidebar
- 前台首页改动范围限定 `app/page.tsx`，不改变页面布局结构
- 权限依赖现有 `merchant-platform` 的 ADMIN/MERCHANT 角色体系

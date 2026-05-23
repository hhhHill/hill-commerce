# Implementation Plan: homepage-management

**Feature**: `homepage-management`
**Plan Status**: draft

## Architecture

```
                               ┌──────────────────────┐
                               │     MySQL            │
                               │  activity_cards      │
                               └────────┬─────────────┘
                                        │
                    ┌───────────────────┼───────────────────┐
                    │                   │                   │
            ┌───────▼──────┐   ┌───────▼──────┐   ┌───────▼──────┐
            │ Admin         │   │ Storefront   │   │ Admin        │
            │ Controller    │   │ Controller   │   │ Page         │
            │ (REST API)    │   │ (REST API)   │   │ /admin/      │
            │ PUT + GET     │   │ GET only     │   │ homepage     │
            └───────────────┘   └──────────────┘   └──────────────┘
                    │                   │
                    ▼                   ▼
            Admin can edit      Frontend homepage
            4 activity cards    reads active cards
```

## Backend Changes

### 新增文件

| 文件 | 职责 |
|------|------|
| `modules/admin/entity/ActivityCard.java` | JPA Entity |
| `modules/admin/mapper/ActivityCardMapper.java` | MyBatis Mapper |
| `modules/admin/service/ActivityCardService.java` | 业务逻辑（查询、批量更新） |
| `modules/admin/web/AdminActivityCardController.java` | Admin API |
| `modules/common/web/StorefrontActivityCardController.java` | 前台公开 API |
| `src/main/resources/db/migration/V003__activity_cards.sql` | 建表 + 初始数据 |

### 修改文件

| 文件 | 改动 |
|------|------|
| `modules/admin/web/AdminActivityCardController.java` | 权限：`@RequireRole({"ADMIN", "MERCHANT"})` |

### API 契约

**GET /api/admin/activity-cards?placement=homepage**

```json
{
  "items": [
    { "id": 1, "placement": "homepage", "position": 0, "title": "限时秒杀",
      "imageUrl": null, "linkUrl": "/search?keyword=限时秒杀", "isActive": true, "sortOrder": 0 }
  ]
}
```

**PUT /api/admin/activity-cards**

```json
// Request body
{
  "cards": [
    { "id": 1, "title": "限时秒杀", "imageUrl": "https://...", "linkUrl": "/search?...", "isActive": true, "sortOrder": 0 },
    { "id": 2, "title": "百亿补贴", "imageUrl": null, "linkUrl": "/search?...", "isActive": true, "sortOrder": 1 },
    ...
  ]
}
// Response: 200 OK, no body
```

**GET /api/storefront/activity-cards?placement=homepage**

```json
{
  "items": [
    { "title": "限时秒杀", "imageUrl": null, "linkUrl": "/search?keyword=限时秒杀" }
    // isActive=false 的不返回
  ]
}
```

## Frontend Changes

### 新增文件

| 文件 | 职责 |
|------|------|
| `app/admin/homepage/page.tsx` | Admin 首页运营管理页面 |
| `features/admin/homepage/homepage-form.tsx` | 卡片编辑表单组件 |
| `lib/admin/homepage-client.ts` | Admin API 客户端 |
| `lib/storefront/homepage-client.ts` | 前台 API 客户端 |

### 修改文件

| 文件 | 改动 |
|------|------|
| `features/admin/admin-sidebar.tsx` | `NAV_ITEMS` 新增"首页运营" → `/admin/homepage` |
| `app/page.tsx` | 删除 `ACTIVITY_CARDS` 硬编码，改为 API 获取 |

### 组件树

```
/admin/homepage/page.tsx
  └── HomepageForm
        ├── ActivityCardField (×4)  ← 标题 input + 图片上传 + 链接 input + 启用 switch
        └── SaveButton
```

管理页面为客户端组件（`"use client"`），首页保持服务端组件。

## Data Migration

- Flyway 迁移脚本 `V003__activity_cards.sql`：
  - 创建 `activity_cards` 表
  - 插入 4 条 homepage 默认数据（对应现有硬编码值）
- 无旧数据需要迁移

## 部署注意事项

- Admin 侧边栏新增条目，ADMIN 和 MERCHANT 均可见
- 前台首页 SSR 时 fetch activity cards，首字节即有卡片内容
- 图片上传复用已有 OSS 上传端点，无需新基础设施

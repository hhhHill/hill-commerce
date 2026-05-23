# Tasks: homepage-management

**Feature**: `homepage-management`

## Backend

- [ ] 创建 Flyway 迁移脚本：`activity_cards` 建表 + 初始化 4 条 homepage 数据
- [ ] 创建 `ActivityCard` Entity
- [ ] 创建 `ActivityCardMapper` MyBatis 接口
- [ ] 创建 `ActivityCardService`（`listByPlacement`、`batchUpdate`）
- [ ] 创建 `AdminActivityCardController`（GET + PUT，权限 ADMIN/MERCHANT）
- [ ] 创建 `StorefrontActivityCardController`（GET，公开，仅返回 isActive=true）

## Frontend

- [ ] 创建 `lib/admin/homepage-client.ts`（Admin API 调用）
- [ ] 创建 `lib/storefront/homepage-client.ts`（前台 API 调用）
- [ ] 创建 `features/admin/homepage/homepage-form.tsx`（4 卡编辑表单）
- [ ] 创建 `app/admin/homepage/page.tsx`（管理页面）
- [ ] 修改 `admin-sidebar.tsx`：NAV_ITEMS 新增"首页运营"
- [ ] 修改 `app/page.tsx`：删除 ACTIVITY_CARDS 硬编码，改为 API 获取

## Verification

- [ ] `npm run build` TypeScript 编译零错误
- [ ] Admin 侧边栏出现"首页运营"入口
- [ ] `/admin/homepage` 可编辑 4 张卡片并保存
- [ ] 保存后刷新前台首页，卡片内容已更新
- [ ] 卡片未上传图片时前台降级显示纯文字
- [ ] 卡片 isActive=false 时前台不展示

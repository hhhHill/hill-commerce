# 后台账户管理与仪表盘任务清单

**Status**: active  
**Parent Plan**: `specs/admin-account-management/plan.md`

## 一、后端 DTO

- [ ] 1.1 新建 `AdminUserDtos`，包含以下记录类：  
  文件：`backend/src/main/java/com/hillcommerce/modules/admin/web/AdminUserDtos.java`  
  - `SalesUserResponse(Long id, String email, String nickname, boolean enabled, LocalDateTime createdAt)`  
  - `CreateSalesRequest(String email, String nickname, String password)`  
  - `ResetPasswordRequest(String password)`  
  - `DisableUserResponse(Long userId, boolean enabled)`

- [ ] 1.2 新建 `AdminDashboardDtos`，包含以下记录类：  
  文件：`backend/src/main/java/com/hillcommerce/modules/admin/web/AdminDashboardDtos.java`  
  - `OrderStatusCount(String status, long count)`  
  - `SalesRankItem(String nickname, int orderCount)`  
  - `DashboardSummaryResponse(Map<String, Long> orderStatusCounts, BigDecimal totalSalesAmount, List<SalesRankItem> salesRanking)`

## 二、后端核心服务

- [ ] 2.1 新建 `AdminUserService`，实现 `listSalesUsers()` 方法  
  文件：`backend/src/main/java/com/hillcommerce/modules/admin/service/AdminUserService.java`  
  要求：查询所有 `SALES` 角色且未被逻辑删除的用户，返回邮箱、昵称、状态、创建时间

- [ ] 2.2 `AdminUserService` 新增 `createSalesUser()` 方法  
  要求：校验邮箱唯一，校验密码 >= 6 位，BCrypt 哈希后插入 `users` 表，分配 `SALES` 角色

- [ ] 2.3 `AdminUserService` 新增 `disableSalesUser()` 方法  
  要求：校验目标用户存在且为 SALES 角色，不可禁用自己，设置 `status = 'DISABLED'`，幂等处理

- [ ] 2.3.1 `AdminUserService` 新增 `enableSalesUser()` 方法  
  要求：校验目标用户存在且为 SALES 角色，设置 `status = 'ACTIVE'`，幂等处理（已启用则直接返回成功）

- [ ] 2.4 `AdminUserService` 新增 `resetPassword()` 方法  
  要求：校验目标用户存在且为 SALES 角色，校验新密码 >= 6 位，BCrypt 哈希后更新 `password_hash`

- [ ] 2.5 新建 `AdminDashboardService`，实现 `getSummary()` 方法  
  文件：`backend/src/main/java/com/hillcommerce/modules/admin/service/AdminDashboardService.java`  
  要求：查询订单状态分布（GROUP BY order_status）、总销售额（SUM payable_amount WHERE status IN PAID/SHIPPED/COMPLETED）、Sales 订单量排行（order_status_histories GROUP BY changed_by JOIN users，Top 10）

## 三、后端控制器

- [ ] 3.1 新建 `AdminUserController`，包含以下端点：  
  文件：`backend/src/main/java/com/hillcommerce/modules/admin/web/AdminUserController.java`  
  - `GET /api/admin/users` — Sales 列表（`requireAdmin` 鉴权）  
  - `POST /api/admin/users` — 新增 Sales（`requireAdmin` 鉴权，校验参数）  
  - `POST /api/admin/users/{id}/disable` — 禁用 Sales（`requireAdmin` 鉴权）  
  - `POST /api/admin/users/{id}/enable` — 启用 Sales（`requireAdmin` 鉴权）  
  - `POST /api/admin/users/{id}/reset-password` — 重置密码（`requireAdmin` 鉴权，校验密码 >= 6 位）

- [ ] 3.2 新建 `AdminDashboardController`，包含以下端点：  
  文件：`backend/src/main/java/com/hillcommerce/modules/admin/web/AdminDashboardController.java`  
  - `GET /api/admin/dashboard/summary` — 仪表盘统计数据（`requireAdmin` 鉴权）

- [ ] 3.3 确认 `requireAdmin` 方法仅校验 `ADMIN` 角色，拒绝 `SALES`  
  实现方式：在控制器中新增 `requireAdmin(Authentication)` 辅助方法，检查 `principal.roles()` 中只含 `ADMIN`

## 四、后端集成测试

- [ ] 4.1 新建 `AdminAccountManagementIntegrationTest`，覆盖以下场景：  
  文件：`backend/src/test/java/com/hillcommerce/admin/AdminAccountManagementIntegrationTest.java`  
  - Admin 查看 Sales 列表（返回 SALES 用户，不含 Admin/Customer）  
  - Admin 新增 Sales（创建成功，可登录）  
  - 重复邮箱新增被拒  
  - 密码不足 6 位被拒  
  - Admin 重置 Sales 密码（新密码可登录，旧密码不可登录）  
  - Admin 禁用 Sales（`status = 'DISABLED'`，重复禁用幂等）  
  - Admin 启用已禁用的 Sales（`status = 'ACTIVE'`，幂等）  
  - Admin 不可禁用自己  
  - 已禁用 Sales 不可登录，启用后可重新登录  
  - Sales 访问用户管理 API 被拒（403）  
  - Dashboard 返回正确统计数据

## 五、前端类型定义

- [ ] 5.1 新增用户管理和仪表盘相关类型  
  文件：`frontend/next-app/src/lib/admin/types.ts`  
  - `SalesUser`：`id, email, nickname, enabled, createdAt`  
  - `SalesUserListResult`：`users: SalesUser[]`  
  - `CreateSalesInput`：`email, nickname, password`  
  - `ResetPasswordInput`：`password`  
  - `DisableResult`：`userId, enabled`  
  - `OrderStatusCount`：`status, count`  
  - `SalesRankItem`：`nickname, orderCount`  
  - `DashboardSummary`：`orderStatusCounts, totalSalesAmount, salesRanking`

## 六、前端 API 层

- [ ] 6.1 后台客户端新增用户管理 API 函数  
  文件：`frontend/next-app/src/lib/admin/client.ts`  
  - `listSalesUsers()` → `GET /api/admin/users`  
  - `createSalesUser(input)` → `POST /api/admin/users`  
  - `disableSalesUser(id)` → `POST /api/admin/users/{id}/disable`  
  - `enableSalesUser(id)` → `POST /api/admin/users/{id}/enable`  
  - `resetSalesPassword(id, password)` → `POST /api/admin/users/{id}/reset-password`

- [ ] 6.2 后台客户端新增仪表盘 API 函数  
  文件：`frontend/next-app/src/lib/admin/client.ts`  
  - `getDashboardSummary()` → `GET /api/admin/dashboard/summary`

- [ ] 6.3 后台服务端新增数据获取函数  
  文件：`frontend/next-app/src/lib/admin/server.ts`  
  - `getServerSalesUsers()` → `GET /api/admin/users`  
  - `getServerDashboardSummary()` → `GET /api/admin/dashboard/summary`

## 七、前端用户管理页面

- [ ] 7.1 新建 Sales 列表客户端组件 `AdminUserList`  
  文件：`frontend/next-app/src/features/admin/user/admin-user-list.tsx`  
  功能：表格展示 Sales（邮箱、昵称、状态标签、创建时间），每行有"重置密码"按钮（弹窗输入新密码）和"禁用"按钮（确认弹窗），已禁用的行显示"启用"按钮，顶部"新增 Sales"链接，空状态

- [ ] 7.2 新建新增 Sales 表单客户端组件 `AdminUserForm`  
  文件：`frontend/next-app/src/features/admin/user/admin-user-form.tsx`  
  功能：邮箱输入、昵称输入、密码输入、错误提示、提交/返回按钮，`useTransition` 加载态

- [ ] 7.3 新建用户管理页（服务端组件）  
  文件：`frontend/next-app/src/app/admin/users/page.tsx`  
  功能：`requireRole(["ADMIN"])` 鉴权，调用 `getServerSalesUsers()`，传递给 `AdminUserList`

- [ ] 7.4 新建新增 Sales 页（服务端组件）  
  文件：`frontend/next-app/src/app/admin/users/new/page.tsx`  
  功能：`requireRole(["ADMIN"])` 鉴权，渲染 `AdminUserForm`

## 八、前端仪表盘页面

- [ ] 8.1 新建仪表盘客户端组件 `AdminDashboard`  
  文件：`frontend/next-app/src/features/admin/dashboard/admin-dashboard.tsx`  
  功能：三个统计卡片——订单状态分布（6 个状态各计数）、总销售额（格式化金额）、Sales 订单量排行（Top 10 列表）

- [ ] 8.2 新建仪表盘页（服务端组件）  
  文件：`frontend/next-app/src/app/admin/dashboard/page.tsx`  
  功能：`requireRole(["ADMIN"])` 鉴权，调用 `getServerDashboardSummary()`，传递给 `AdminDashboard`

## 九、后台导航扩展

- [ ] 9.1 `AdminShell` 导航按角色过滤  
  文件：`frontend/next-app/src/features/admin/catalog/admin-shell.tsx`  
  变更：`NAV_ITEMS` 改为根据 `user.roles` 动态过滤，Admin 额外看到"用户管理"和"仪表盘"，Sales 只看原有的 4 项

## 十、联调验证

- [ ] 10.1 运行后端集成测试，确认全部通过

- [ ] 10.2 启动前端验证完整链路：  
  - Admin 登录 → 导航看到"用户管理"和"仪表盘" → Sales 登录看不到这两个导航  
  - Admin → `/admin/users` → 查看 Sales 列表 → 新增 Sales → 用新账户登录成功  
  - Admin → 重置某 Sales 密码 → 新密码可登录，旧密码不可登录  
  - Admin → 禁用某 Sales → 该账户无法登录  
  - Admin → `/admin/dashboard` → 看到三个统计卡片数据正确  
  - Sales → 直接访问 `/admin/users` 或 `/admin/dashboard` → 被拒绝或重定向

## 完成标准

- 后端所有集成测试通过
- Admin 可新增、查看、禁用 Sales 账户
- Admin 可重置 Sales 密码
- 被禁用的 Sales 无法登录
- Admin 可查看仪表盘统计数据
- Sales 无法访问用户管理和仪表盘

# Feature Specification: auth-permission

**Feature**: `auth-permission`  
**Status**: active  
**Parent Baseline**: `specs/hill-commerce-mvp/spec.md`

## Purpose

定义商城 MVP 的认证、会话、角色边界与受保护页面规则。

## Scope

### In Scope

- 邮箱注册、登录、注销
- Session/Cookie 登录态
- `CUSTOMER`、`SALES`、`ADMIN` 角色边界
- 前台受保护页与后台受保护页
- 无权限跳转
- 登录成功与失败日志

### Out of Scope

- SSO
- OAuth
- 多因子认证
- 复杂 RBAC 权限矩阵
- 用户资料高级编辑

## Business Rules

- 后端是唯一认证事实源
- 前端不保存 token，不自行推导角色真相
- 浏览器通过 Session Cookie 维持登录态
- 前台注册用户默认授予 `CUSTOMER`
- 后台至少支持 `ADMIN` 与 `SALES` 边界
- 已登录用户访问 `/login` 或 `/register` 时应被重定向离开认证页
- 登录成功与失败必须写入应用日志，禁止记录明文密码
- 成功登录后必须更新用户最近登录时间 `lastLoginAt`；失败登录不得更新该字段
- 运行期密码凭证只接受 `BCrypt` 哈希格式；无盐 `SHA-256` 不属于合法登录凭证格式
- 遇到历史无盐 `SHA-256` 密码哈希时，系统必须拒绝认证，不得继续作为兼容登录路径
- 本地 bootstrap 管理员账号允许保留固定初始密码，但其种子存储值必须是 `BCrypt` 哈希
- 种子数据、注册流程、认证校验、测试基线必须使用同一密码哈希策略，不得出现“种子用一种、运行期验另一种”的双重规则
- 认证阶段允许使用包含 `passwordHash` 的内部认证对象完成密码校验
- 认证成功后写入 `SecurityContext` / `HttpSession` 的会话期 principal 不得保留 `passwordHash` 或其他可用于离线破解的密码派生值
- 会话期 principal 仅允许保留鉴权和页面展示所必需的最小身份字段，例如 `userId`、`email`、`nickname`、`status`、`roles`

## Session Security Boundary

- 认证阶段 principal 与会话阶段 principal 必须显式分离，不得复用同一个含敏感凭据字段的对象贯穿整个 Session 生命周期
- `HttpSession` 中持久化的 `SecurityContext` 不得包含 `BCrypt` 哈希、历史弱哈希、明文密码或等价敏感凭据材料
- 控制器、鉴权辅助函数和受保护页面读取当前用户时，应依赖会话期 principal 的最小身份视图，而不是依赖认证阶段的密码载荷对象
- 若未来扩展 remember-me、分布式 session 或 session 持久化，仍需遵守“会话对象不携带 passwordHash”的同一规则

## Password Policy Boundary

- 本 feature 当前唯一批准的密码存储算法为 `BCrypt`
- 本 feature 不定义多算法协商、登录时自动迁移、透明升级或兼容窗口
- 若存在历史弱哈希账号，处理方式应为失效后重置密码，而不是继续允许旧哈希直接登录
- 固定初始密码仅可作为本地开发 bootstrap 便利，不得推导出生产运行期应接受弱哈希凭证

## Primary Paths

- 用户注册并登录
- 已登录用户访问 `/account`
- Sales/Admin 访问 `/admin`
- Customer 访问 `/admin` 被拒绝并跳转 `/forbidden`

## Acceptance Criteria

- Customer 可注册、登录、注销，并访问 `/account`
- Sales/Admin 可登录并访问 `/admin`
- 未登录访问 `/account` 或 `/admin` 时跳转到 `/login`
- 无权限访问后台时进入 `/forbidden` 或等价页面
- 登录成功与失败均有可验证日志
- 登录成功后，`users.last_login_at` 必须被更新为本次成功登录时间；失败登录不得改写该字段
- 使用 `BCrypt` 哈希存储的账号可按既定流程登录
- 使用无盐 `SHA-256` 哈希存储的账号必须登录失败，且该失败被视为符合预期的安全行为
- 本地 bootstrap 管理员账号若存在固定初始密码，其数据库种子值必须为 `BCrypt` 哈希而非 `SHA2(...)` 或等价弱哈希表达式
- 登录成功后，会话中的 principal 仍可支持 `/api/auth/me`、`/api/admin/**` 等既有访问路径，但不得暴露或持有 `passwordHash`

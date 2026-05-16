# Implementation Plan: auth-permission

**Feature**: `auth-permission`  
**Status**: active  

## Summary

在当前后端 Session 认证骨架和前端同源代理基础上，完成注册、登录、注销、当前用户读取、角色边界和受保护页面闭环。

## Current Repository Reality

- 后端已存在 `modules/user`、`framework/security` 相关实现
- 前端已存在 `src/app/(auth)`、`src/app/account`、`src/app/admin`、`src/app/api/auth/*`
- `middleware.ts` 已存在

## Technical Boundaries

- 后端继续作为认证事实源
- Spring Security 使用基于 Session 的认证
- Next.js Route Handlers 代理认证请求
- `middleware.ts` 仅做快速拦截，不在 middleware 中请求后端
- 精细角色校验在服务端组件或服务端辅助函数中完成
- 运行期密码校验只接受 `BCrypt`；不得在认证链路中保留无盐 `SHA-256` 回退逻辑
- 种子管理员账号如保留固定初始密码，必须在种子阶段直接写入 `BCrypt` 哈希值
- 历史弱哈希账号不做“登录即升级”或透明兼容，统一通过失效后重置密码处理
- 认证阶段 principal 与会话阶段 principal 分离：前者可携带 `passwordHash` 用于校验，后者写入 `SecurityContext` 时必须去敏
- 会话期 principal 只保留 `userId`、`email`、`nickname`、`status`、`roles` 等最小身份字段
- 成功登录后需要更新 `users.last_login_at`，且该更新只在认证成功路径触发

## Main Delivery Areas

- 注册/登录/注销 API
- `GET /api/auth/me`
- 前端登录/注册页
- `/account` 与 `/admin` 保护逻辑
- `/forbidden` 页面
- 登录成功/失败日志
- 密码哈希策略统一：注册编码、种子数据、认证验证、测试基线一致
- 历史无盐 `SHA-256` 凭证拒绝策略与验证用例
- Session principal 最小化：认证成功后不再把 `passwordHash` 持久化到 `HttpSession`
- 控制器与鉴权读取路径改为依赖会话期最小身份视图，而非依赖认证阶段 principal 的密码字段
- 登录成功副作用收口：更新 `lastLoginAt`，并验证失败登录不会污染该字段

## Risks

- 同源 Cookie 与代理行为不一致
- middleware 与服务端角色校验职责混淆
- 种子管理员密码与运行期密码编码策略不一致
- 若继续保留 `SHA-256` 兼容分支，会把历史迁移措施固化为正式认证规则
- 若测试仍以 `SHA-256` 管理员登录成功为基线，会持续放大错误安全假设
- 若认证阶段与会话阶段继续复用同一 principal，对 session 泄露、序列化落盘和分布式会话复制的暴露面会被放大
- 若 `lastLoginAt` 不更新，账户最近登录状态与审计预期会继续失真；若失败登录也更新，则会污染账户状态事实

## Implementation Notes

- 本次规范要求先统一认证规则，再允许实现侧移除 `SHA-256` 兼容分支
- 本地开发体验可以通过 `BCrypt` 形式的固定种子密码保留，不应通过降低运行期密码要求实现
- 验证应覆盖正向场景（`BCrypt` 可登录）与负向场景（无盐 `SHA-256` 不可登录）
- 验证还应覆盖会话安全场景：登录成功后，`SPRING_SECURITY_CONTEXT` 中的 principal 不应持有 `passwordHash`
- 验证还应覆盖状态更新场景：成功登录更新时间戳，失败登录保持原值

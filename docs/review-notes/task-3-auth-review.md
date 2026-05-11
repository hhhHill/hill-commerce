# Task 3 Review Notes: 认证、会话与权限体系

**Review Date**: 2026-05-11
**Source**: `specs/hill-commerce-mvp/tasks.md` — Phase 1, Task 3
**Scope**: 后端 SecurityConfig、AuthController、认证服务、实体、迁移脚本；前端 Middleware、Route Handlers、Auth Lib、受保护页面

---

## 严重问题（4个）

### 1. Admin 种子密码使用 SHA-256，非 BCrypt

- **文件**: `backend/src/main/resources/db/migration/V3__seed_admin_account.sql:8`
- **相关**: `backend/src/main/java/com/hillcommerce/modules/user/service/PasswordService.java:29`

`V3__seed_admin_account.sql` 使用 `SHA2('Admin@123456', 256)` 写入种子密码。`PasswordService.matches()` 中有 SHA-256 fallback（检测 BCrypt 前缀 `$2a$`/`$2b$`/`$2y$`，非 BCrypt 则回退到 SHA-256 比较）。SHA-256 无盐，彩虹表攻击风险高。

迁移脚本自身注释也标注了：*"Task 3 must align the runtime password encoder and rotate this seed if needed."*，但尚未处理。

**建议**: 将种子密码用 BCrypt 重哈希；确认所有运行时密码均为 BCrypt 后，移除 PasswordService 中的 SHA-256 fallback。

---

### 2. BCrypt 哈希被存入 HttpSession

- **文件**: `backend/src/main/java/com/hillcommerce/modules/user/security/AppUserPrincipal.java:46-48`

```java
@Override
public String getPassword() {
    return authUser.passwordHash();  // BCrypt hash 存入 Session
}
```

`AppUserPrincipal` 通过 `HttpSessionSecurityContextRepository` 序列化到 HTTP Session 中，整个 Session 生命周期内密码哈希一直存储在内存/序列化数据中。Session 一旦泄露，攻击者可直接获取 BCrypt 哈希进行离线破解。

**建议**: `getPassword()` 返回 `null` 或空字符串（认证成功后不再需要密码），或使用自定义 `SecurityContextRepository` 在持久化前擦除密码。

---

### 3. Session 固定攻击（Session Fixation）防护被绕过

- **文件**: `backend/src/main/java/com/hillcommerce/modules/user/web/AuthController.java:82-86`

```java
SecurityContext context = SecurityContextHolder.createEmptyContext();
context.setAuthentication(authentication);
SecurityContextHolder.setContext(context);
securityContextRepository.saveContext(context, httpServletRequest, httpServletResponse);
// 缺少: request.changeSessionId()
```

`AuthController.login()` 手动创建 SecurityContext 并通过 `SecurityContextRepository` 保存，绕过了 Spring Security 默认的 `SessionFixationProtectionStrategy`，该策略通常在认证成功后调用 `request.changeSessionId()` 来防止 Session 固定攻击。

**建议**: 在 `saveContext` 之后调用 `httpServletRequest.changeSessionId()`。

---

### 4. `lastLoginAt` 字段从未更新

- **文件**: `backend/src/main/java/com/hillcommerce/modules/user/web/AuthController.java:63-97`
- **相关**: `backend/src/main/java/com/hillcommerce/modules/user/entity/UserEntity.java:18`

数据库和实体中都有 `lastLoginAt` 字段，但 `AuthController.login()` 中未对其进行更新，登录后该字段始终为 `NULL`。

**建议**: 在登录成功后更新 `users.last_login_at`。

---

## 中等问题（5个）

### 5. 自定义 Session Cookie 名称配置未生效

- **文件**: `backend/src/main/resources/application.yml:42`
- **相关**: `frontend/next-app/middleware.ts:8`、`frontend/next-app/src/lib/auth/proxy.ts:38`

`application.yml` 中配置了 `hill.security.session-cookie-name: HILL_COMMERCE_SESSION`，但 Spring Boot 实际读取的是 `server.servlet.session.cookie.name`，该自定义属性未被任何代码消费。实际 Cookie 名称仍为 `JSESSIONID`。前端也硬编码使用 `JSESSIONID`，功能未受影响——但配置项本身是无效的"死代码"。

**建议**: 要么改为 `server.servlet.session.cookie.name: HILL_COMMERCE_SESSION` 并同步前端常量；要么删除该自定义配置。

---

### 6. CSRF 保护完全禁用

- **文件**: `backend/src/main/java/com/hillcommerce/framework/security/SecurityConfig.java:25`

```java
.csrf(AbstractHttpConfigurer::disable)
```

基于 Session 认证 + `credentials: "include"` 的前端模式下，禁用 CSRF 意味着后端 `/api/*` 端点无任何跨站请求伪造防护。Next.js 同源代理提供了一定缓解（API Route Handlers 与页面同源），但未做 origin 白名单校验。

**建议**: 至少对写操作（POST/PUT/DELETE）启用 CSRF Token 机制，或在代理层校验 Origin/Referer。

---

### 7. 登录页明文展示管理员凭据

- **文件**: `frontend/next-app/src/app/(auth)/login/login-form.tsx:30`

```tsx
<p>管理员测试账号：</p>
<p>admin@hill-commerce.local / Admin@123456</p>
```

本地 MVP 演示可接受，但不应进入任何非本地环境。

**建议**: 通过环境变量控制是否展示，或在生产构建中移除。

---

### 8. 登录/注册接口无速率限制

无任何限流机制（无 Spring Security 限流 Filter、无 Nginx `limit_req`、无自定义注解），登录和注册端点可被暴力枚举。

**建议**: 添加 IP 级别速率限制（MVP 阶段可简单实现，后续接 Redis）。

---

### 9. `getCookieHeader()` 重复实现

- **文件**: `frontend/next-app/src/lib/auth/server.ts:52-58`
- **文件**: `frontend/next-app/src/lib/admin/server.ts:46-51`

完全相同的逻辑在两处各写了一遍。

**建议**: 抽取到共享工具模块（如 `lib/cookie-utils.ts`）。

---

## 轻微问题（5个）

### 10. Middleware 只看 Cookie 是否存在，不验证有效性

- **文件**: `frontend/next-app/middleware.ts:8`

```ts
const hasSessionCookie = Boolean(request.cookies.get("JSESSIONID")?.value);
```

只检查 `JSESSIONID` Cookie 是否存在，不验证 Session 是否在后端仍然有效。过期/损坏的 Session 会先通过 Middleware，页面加载后再由服务端 `requireUser()` 拦截重定向，体验不佳。

**建议**: MVP 阶段可接受；后续可考虑在 Middleware 中调用 `/api/auth/me` 做轻量验证（注意性能）。

---

### 11. `LoginRequest` DTO 缺少密码长度校验

- **文件**: `backend/src/main/java/com/hillcommerce/modules/user/web/AuthDtos.java:21-23`

`RegisterRequest.password` 有 `@Size(min=8, max=64)`，但 `LoginRequest.password` 只有 `@NotBlank`。空字符串能通过校验，虽然一定会认证失败。

**建议**: 给 `LoginRequest.password` 也加上 `@Size(min=1, max=64)`。

---

### 12. `getCredentials()` 为 null 时返回字符串 "null"

- **文件**: `backend/src/main/java/com/hillcommerce/modules/user/security/AppAuthenticationProvider.java:26`

```java
String rawPassword = String.valueOf(authentication.getCredentials());
```

`String.valueOf(null)` 返回字符串 `"null"`，不会匹配任何哈希，不造成安全漏洞但不够严谨。

**建议**: 显式检查 `authentication.getCredentials() == null`。

---

### 13. 注册后需手动重新登录

- **文件**: `frontend/next-app/src/app/(auth)/register/page.tsx:23`

注册成功后跳转到 `/login?email=...`，用户需再次输入密码登录。这是已知的 MVP 设计决策（页面文案已说明），不算 bug，但可作为体验优化点。

---

### 14. 注销时未清理 SecurityContextRepository

- **文件**: `backend/src/main/java/com/hillcommerce/modules/user/web/AuthController.java:109-113`

```java
session.invalidate();
SecurityContextHolder.clearContext();
// 缺少: 向 SecurityContextRepository 写入空 Context
```

注销时调用了 `session.invalidate()` 和 `SecurityContextHolder.clearContext()`，但未显式通过 `securityContextRepository.saveContext()` 持久化空上下文。

**建议**: 通常 `session.invalidate()` 已经足够（Session 销毁后 Repository 无法读取），但显式保存空上下文更规范。

---

## 做得好的地方

| 项目 | 说明 |
|---|---|
| 登录成功/失败日志 | 不记录明文密码，记录了 email/roles/remoteAddr |
| 运行时密码编码 | BCrypt（`BCryptPasswordEncoder`），strength 默认 10 |
| 角色权限控制 | `/api/admin/**` → `hasAnyRole("ADMIN", "SALES")`，边界清晰 |
| JSON 格式 401/403 | 配置了 `jsonAuthenticationEntryPoint` 和 `jsonAccessDeniedHandler` |
| 集成测试覆盖 | `AuthFlowIntegrationTest` 覆盖注册、登录、角色校验、注销、错误密码 |
| 服务端鉴权 | 所有受保护页面均通过 `requireUser()` / `requireRole()` 在服务端鉴权 |
| 403 页面 | `/forbidden` 路由有清晰的权限不足说明 |
| 注销完整 | Session invalidate + JSESSIONID Cookie 清除 |
| 邮箱唯一性 | 数据库 `uk_users_email` UNIQUE 约束 + 应用层 `DuplicateKeyException` 捕获 |
| 默认 CUSTOMER 角色 | 注册时自动分配 |
| 同源代理模式 | Next.js Route Handlers 代理后端请求，避免跨域 Cookie 问题 |

---

## 文件清单

### 后端（15 个文件）

| 文件 | 用途 |
|---|---|
| `framework/security/SecurityConfig.java` | Spring Security 配置 |
| `modules/user/web/AuthController.java` | 登录/注册/注销/当前用户 API |
| `modules/user/web/AuthDtos.java` | 请求/响应 DTO |
| `modules/admin/web/AdminAuthController.java` | 后台认证验证端点 |
| `modules/user/entity/UserEntity.java` | 用户实体 |
| `modules/user/entity/RoleEntity.java` | 角色实体 |
| `modules/user/entity/UserRoleEntity.java` | 用户角色关联实体 |
| `modules/user/model/AuthUser.java` | 认证域模型 |
| `modules/user/security/AppUserPrincipal.java` | UserDetails 实现 |
| `modules/user/security/AppUserDetailsService.java` | UserDetailsService 实现 |
| `modules/user/security/AppAuthenticationProvider.java` | 认证 Provider |
| `modules/user/service/UserAccountService.java` | 用户账户服务 |
| `modules/user/service/PasswordService.java` | 密码编码/验证 |
| `modules/user/mapper/UserMapper.java` | 用户 Mapper（含自定义角色查询） |
| `modules/user/mapper/RoleMapper.java` | 角色 Mapper |
| `modules/user/mapper/UserRoleMapper.java` | 用户角色 Mapper |

### 数据库迁移（3 个文件）

| 文件 | 用途 |
|---|---|
| `db/migration/V1__init_schema.sql` | 全量表结构（users, roles, user_roles 等） |
| `db/migration/V2__seed_roles.sql` | 种子角色（CUSTOMER, SALES, ADMIN） |
| `db/migration/V3__seed_admin_account.sql` | 种子 Admin 账户 |

### 测试（1 个文件）

| 文件 | 用途 |
|---|---|
| `test/.../auth/AuthFlowIntegrationTest.java` | 认证流程集成测试 |

### 前端（18 个文件）

| 文件 | 用途 |
|---|---|
| `middleware.ts` | 路由保护中间件 |
| `lib/auth/types.ts` | 类型定义 |
| `lib/auth/server.ts` | 服务端鉴权（getSessionUser, requireUser, requireRole） |
| `lib/auth/client.ts` | 客户端 API（register, login, logout） |
| `lib/auth/proxy.ts` | 后端请求代理 |
| `lib/auth/proxy-response.ts` | 代理响应处理 |
| `lib/admin/types.ts` | 后台类型定义 |
| `lib/admin/server.ts` | 后台服务端数据获取 |
| `lib/admin/client.ts` | 后台客户端 API |
| `lib/config.ts` | 后端地址配置 |
| `app/api/auth/login/route.ts` | 登录代理 |
| `app/api/auth/register/route.ts` | 注册代理 |
| `app/api/auth/logout/route.ts` | 注销代理 |
| `app/api/auth/me/route.ts` | 当前用户代理 |
| `app/api/admin/[...path]/route.ts` | 后台接口代理 |
| `app/(auth)/login/page.tsx` | 登录页 |
| `app/(auth)/login/login-form.tsx` | 登录表单 |
| `app/(auth)/register/page.tsx` | 注册页 |
| `app/account/page.tsx` | 前台受保护页 |
| `app/admin/page.tsx` | 后台总览页 |
| `app/forbidden/page.tsx` | 403 页面 |
| `components/logout-button.tsx` | 注销按钮 |
| `features/admin/catalog/admin-shell.tsx` | 后台布局 Shell |

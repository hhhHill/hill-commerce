# Implementation Plan: operations-observability

**Feature**: `operations-observability`  
**Status**: active  

## Summary

在 `modules/logging` 包下统一实现三类日志采集与查询。复用已有的三张日志表（`login_logs`、`operation_logs`、`product_view_logs`），不新建数据库表。操作日志通过 AOP 注解自动采集，登录日志在 `AuthController` 中内联写入，浏览日志由前端主动上报。本期交付范围为日志采集、查询 API、商品详情页浏览上报与集成测试，不包含后台日志列表页面。

## Technical Decisions

### 模块组织

新建 `modules/logging` 统一收归 Entity / Mapper / Service / Controller / AOP，避免日志职责散落在 user/admin/product 模块中。

### 操作日志采集：AOP 注解

自定义 `@OperationLog(action = "DISABLE_USER", targetType = "USER")` 注解，切面从方法参数中提取 `targetId`，从 `SecurityContext` 获取操作者信息。切面必须先执行业务方法，业务异常原样抛出；仅日志采集和写入阶段的异常可被 catch 后 `log.error`，不向上抛出，保证主业务不受日志写入失败影响。

### 登录日志采集：内联调用

在 `AuthController.login()` 中直接调用 `LoggingService.recordLogin()`，因为需要区分成功/失败两条路径，AOP 反而不自然。`recordLogin()` 需包裹独立 try-catch，日志写入失败只记录应用日志，不改变成功/失败登录的原始响应语义。

### 浏览日志采集：前端上报

前端 `product-view-beacon.tsx` 从 CustomEvent 改为直接 `fetch("POST /api/storefront/view-log")`。`src/lib/storefront/logging.ts` 新增 `getOrCreateAnonymousId()`：首次生成匿名标识并持久化，同一浏览器复用；`reportViewLog()` 请求体包含 `productId`、`categoryId`、`anonymousId`。若存在登录态，后端以认证上下文中的 `userId` 为准；若匿名态且无法生成 `anonymousId`，前端跳过上报。不采集停留时长（MVP 不做），进入页面即上报。

### 日志查询

各 Controller 用 JdbcTemplate 手写 SQL，默认返回最近 100 条，支持简单 `?key=value` 筛选。不做复杂分页和排序 UI。三类查询统一返回 `items` 数组，不返回总数与分页元数据。

### 权限控制

- `/api/admin/login-logs`、`/api/admin/operation-logs` 仅 Admin 可访问（`requireAdmin`）
- `/api/admin/view-logs` 允许 Admin 和 Sales（`requireRole(["ADMIN", "SALES"])`）
- `/api/storefront/view-log` 公开，并在 `SecurityConfig.permitAll()` 中显式放行
- `POST /api/storefront/view-log` 不要求认证，但若存在认证上下文则提取 `userId`

## Backend Design

### 新增文件清单

| 文件 | 职责 |
|------|------|
| `modules/logging/entity/LoginLogEntity.java` | 登录日志实体，对应 `login_logs` 表 |
| `modules/logging/entity/OperationLogEntity.java` | 操作日志实体，对应 `operation_logs` 表 |
| `modules/logging/entity/ProductViewLogEntity.java` | 浏览日志实体，对应 `product_view_logs` 表 |
| `modules/logging/mapper/LoginLogMapper.java` | MyBatis-Plus BaseMapper |
| `modules/logging/mapper/OperationLogMapper.java` | MyBatis-Plus BaseMapper |
| `modules/logging/mapper/ProductViewLogMapper.java` | MyBatis-Plus BaseMapper |
| `modules/logging/service/LoggingService.java` | 统一写入服务（三个 Mapper 的薄封装） |
| `modules/logging/web/LoggingController.java` | 登录日志 + 操作日志 + 浏览日志 查询接口 + 浏览日志上报接口 |
| `modules/logging/web/LoggingDtos.java` | 请求/响应 DTO |
| `modules/logging/aop/OperationLog.java` | @OperationLog 注解定义 |
| `modules/logging/aop/OperationLogAspect.java` | 切面实现 |

### 修改文件清单

| 文件 | 变更 |
|------|------|
| `modules/user/web/AuthController.java` | `login()` 成功/失败路径调用 `LoggingService.recordLogin()` |
| `framework/security/SecurityConfig.java` | 放行 `/api/storefront/view-log` |
| `modules/admin/web/AdminUserController.java` | 挂 `@OperationLog` |
| `modules/product/web/ProductCategoryAdminController.java` | 挂 `@OperationLog` |
| `modules/product/web/ProductAdminController.java` | 挂 `@OperationLog` |
| `modules/order/web/ShipmentController.java` | 挂 `@OperationLog` |

### 数据流

```
POST /api/auth/login
  → AuthController.login()
    → authenticationManager.authenticate()
      → 成功: LoggingService.recordLogin(SUCCESS)
      → 失败: LoggingService.recordLogin(FAILURE)

Admin 写操作（例: POST /api/admin/users/{id}/disable）
  → AdminUserController.disableUser()
    → @OperationLog(action = "DISABLE_USER", targetType = "USER")
      → OperationLogAspect.around()
        → LoggingService.recordOperation()

POST /api/storefront/view-log
  → LoggingController.reportView()
    → LoggingService.recordProductView()
```

### @OperationLog 注解设计

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {
    String action();        // e.g. "DISABLE_USER"
    String targetType();    // e.g. "USER"
    String targetIdExpr() default "";  // SpEL, e.g. "#id" 从方法参数提取
    String detail() default "";        // 可选的补充描述
}
```

切面逻辑：
1. 从 SecurityContext 获取当前操作者（userId, roles）
2. 从 HttpServletRequest 获取 IP
3. 解析 targetIdExpr 拿到 targetId
4. 先执行 `proceed()`，若业务方法抛异常则原样向上抛出
5. 业务成功后调用 `LoggingService.recordOperation()`
6. 若日志写入抛异常，仅记录错误日志，不影响已完成的主业务响应

## Frontend Design

### 修改文件清单

| 文件 | 变更 |
|------|------|
| `src/lib/storefront/logging.ts` | 保留现有 CustomEvent 浏览埋点；新增 `getOrCreateAnonymousId()` 与 `reportViewLog()` |
| `src/features/storefront/catalog/product-view-beacon.tsx` | 调用 `reportViewLog()` 替代 `recordBrowseEvent()` |
| `src/lib/admin/types.ts` | 新增 `LoginLogEntry`、`OperationLogEntry`、`ProductViewLogEntry`、`LoginLogListResult`、`OperationLogListResult`、`ProductViewLogListResult` 类型 |
| `src/lib/admin/client.ts` | 新增 `getLoginLogs()`、`getOperationLogs()`、`getViewLogs()` 查询函数 |
| `src/lib/admin/server.ts` | 新增 `getServerLoginLogs()`、`getServerOperationLogs()`、`getServerViewLogs()` |

### 改造 product-view-beacon

```
当前：dispatchEvent(CustomEvent) → 无后端接收
改造：useEffect 中调用 reportViewLog({ productId, categoryId, anonymousId })
```

其余 `browse-event-link.tsx`、`search-form.tsx` 等仍保留 `recordBrowseEvent()` 机制，不纳入本 feature 改造范围。

## Test Strategy

### 后端集成测试

新建 `LoggingIntegrationTest`，覆盖：

| 测试用例 | 验证点 |
|---------|--------|
| 登录成功写入日志 | login_logs 新增一行，login_result = SUCCESS |
| 登录失败写入日志 | login_logs 新增一行，login_result = FAILURE，user_id 为 null |
| Admin 查询登录日志 | 可按 email 筛选 |
| @OperationLog 自动写入 | 触发禁用用户 → operation_logs 新增一行 |
| 操作日志写入失败不阻断主业务 | Mock mapper 抛异常 → 主操作正常返回 200 |
| Admin 查询操作日志 | 可按 operatorUserId、actionType 筛选 |
| 商品浏览上报（登录用户） | product_view_logs 新增一行，user_id 有值 |
| 商品浏览上报（匿名用户） | product_view_logs 新增一行，anonymous_id 有值 |
| 匿名浏览上报端点公开 | 未登录请求 `/api/storefront/view-log` 返回成功而非 401 |
| 登录日志写入失败不改变登录语义 | 日志写入异常时，成功登录仍成功，失败登录仍返回认证失败 |
| Admin 查询浏览日志 | 返回最近 100 条 |
| Sales 查询浏览日志 | 返回 200 |
| Sales 查询操作日志被拒 | 返回 403 |
| Sales 查询登录日志被拒 | 返回 403 |

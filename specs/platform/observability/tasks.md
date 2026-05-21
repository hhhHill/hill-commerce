# Tasks: operations-observability

**Status**: active  
**Parent Plan**: `specs/operations-observability/plan.md`

## 一、后端 Entity 与 Mapper

- [ ] 1.1 新建 `LoginLogEntity`  
  文件：`backend/src/main/java/com/hillcommerce/modules/logging/entity/LoginLogEntity.java`  
  字段：id, userId, emailSnapshot, roleSnapshot, loginResult, ipAddress, userAgent, loginAt  
  注解：`@TableName("login_logs")`，`@TableId(type = IdType.AUTO)`

- [ ] 1.2 新建 `OperationLogEntity`  
  文件：`backend/src/main/java/com/hillcommerce/modules/logging/entity/OperationLogEntity.java`  
  字段：id, operatorUserId, operatorRole, actionType, targetType, targetId, actionDetail, ipAddress, createdAt

- [ ] 1.3 新建 `ProductViewLogEntity`  
  文件：`backend/src/main/java/com/hillcommerce/modules/logging/entity/ProductViewLogEntity.java`  
  字段：id, userId, anonymousId, productId, categoryId, viewedAt

- [ ] 1.4 新建 `LoginLogMapper`、`OperationLogMapper`、`ProductViewLogMapper`  
  文件：`backend/src/main/java/com/hillcommerce/modules/logging/mapper/`  
  每个：`@Mapper interface XxxMapper extends BaseMapper<XxxEntity> {}`

## 二、后端核心服务

- [ ] 2.1 新建 `LoggingService`  
  文件：`backend/src/main/java/com/hillcommerce/modules/logging/service/LoggingService.java`  
  方法：`recordLogin()`、`recordOperation()`、`recordProductView()`  
  注入：三个 Mapper

## 三、后端 AOP

- [ ] 3.1 新建 `@OperationLog` 注解  
  文件：`backend/src/main/java/com/hillcommerce/modules/logging/aop/OperationLog.java`  
  属性：`action()`, `targetType()`, `targetIdExpr()` default "", `detail()` default ""

- [ ] 3.2 新建 `OperationLogAspect` 切面  
  文件：`backend/src/main/java/com/hillcommerce/modules/logging/aop/OperationLogAspect.java`  
  逻辑：从 SecurityContext 拿操作者 → 从 request 拿 IP → 解析 SpEL → 先执行 `proceed()` → 业务异常原样抛出 → 业务成功后调 `LoggingService.recordOperation()` → 仅日志写入异常 catch 并记录 `log.error`

- [ ] 3.3 注入点：给以下 Controller 方法挂 `@OperationLog`  
  - `AdminUserController.createUser()` — `CREATE_USER / USER`
  - `AdminUserController.disableUser()` — `DISABLE_USER / USER`
  - `AdminUserController.enableUser()` — `ENABLE_USER / USER`
  - `AdminUserController.resetPassword()` — `RESET_PASSWORD / USER`
  - `ProductCategoryAdminController.create*()` — `CREATE_CATEGORY / CATEGORY`
  - `ProductCategoryAdminController.update*()` — `UPDATE_CATEGORY / CATEGORY`
  - `ProductCategoryAdminController.delete*()` — `DELETE_CATEGORY / CATEGORY`
  - `ProductAdminController.create*()` — `CREATE_PRODUCT / PRODUCT`
  - `ProductAdminController.update*()` — `UPDATE_PRODUCT / PRODUCT`
  - `ProductAdminController.delete*()` — `DELETE_PRODUCT / PRODUCT`
  - `ShipmentController` 发货方法 — `SHIP_ORDER / ORDER`

## 四、后端安全配置

- [ ] 4.1 修改 `SecurityConfig`  
  文件：`backend/src/main/java/com/hillcommerce/framework/security/SecurityConfig.java`  
  - 将 `POST /api/storefront/view-log` 加入 `permitAll()`
  - 保持 `/api/admin/**` 现有鉴权规则不变

## 五、后端 Controller 与 DTO

- [ ] 5.1 新建 `LoggingDtos`  
  文件：`backend/src/main/java/com/hillcommerce/modules/logging/web/LoggingDtos.java`  
  包含：
  - `LoginLogEntry(Long id, Long userId, String emailSnapshot, String roleSnapshot, String loginResult, String ipAddress, String userAgent, LocalDateTime loginAt)`
  - `LoginLogListResult(List<LoginLogEntry> items)`
  - `OperationLogEntry(Long id, Long operatorUserId, String operatorRole, String actionType, String targetType, String targetId, String actionDetail, String ipAddress, LocalDateTime createdAt)`
  - `OperationLogListResult(List<OperationLogEntry> items)`
  - `ProductViewLogEntry(Long id, Long userId, String anonymousId, Long productId, Long categoryId, LocalDateTime viewedAt)`
  - `ProductViewLogListResult(List<ProductViewLogEntry> items)`
  - `ViewLogRequest(Long productId, Long categoryId, String anonymousId)`

- [ ] 5.2 新建 `LoggingController`  
  文件：`backend/src/main/java/com/hillcommerce/modules/logging/web/LoggingController.java`  
  - `GET /api/admin/login-logs` — Admin 鉴权，JdbcTemplate 查询，支持 `?email=` `?result=`
  - `GET /api/admin/operation-logs` — Admin 鉴权，JdbcTemplate 查询，支持 `?operatorUserId=` `?actionType=`
  - `GET /api/admin/view-logs` — Admin/Sales 鉴权，JdbcTemplate 查询，支持 `?productId=` `?categoryId=`
  - `POST /api/storefront/view-log` — 公开；若 Authentication 中存在已登录用户则写 `userId`，否则要求请求体提供 `anonymousId`
  - 三类查询统一返回 `{ items: [...] }`，不返回总数或分页元数据

## 六、AuthController 集成

- [ ] 6.1 `AuthController.login()` 注入 `LoggingService`  
  - 认证成功：调 `recordLogin(userId, email, roleSnapshot, "SUCCESS", ip, userAgent)`
  - 认证失败（catch AuthenticationException 中）：调 `recordLogin(null, email, "UNKNOWN", "FAILURE", ip, userAgent)`
  - 提取 userAgent：`request.getHeader("User-Agent")`
  - `recordLogin()` 调用必须独立 catch，日志写入失败不影响原始登录结果

## 七、后端集成测试

- [ ] 7.1 新建 `LoggingIntegrationTest`  
  文件：`backend/src/test/java/com/hillcommerce/logging/LoggingIntegrationTest.java`  
  覆盖：
  - 登录成功 → login_logs 新增 SUCCESS 记录
  - 登录失败 → login_logs 新增 FAILURE 记录
  - Admin 查询登录日志
  - @OperationLog → 操作日志写入
  - AOP 异常不阻断主业务
  - 登录日志写入失败时，成功登录/失败登录的原始响应语义不变
  - Admin 查询操作日志
  - 商品浏览上报（登录/匿名）
  - 匿名访问 `POST /api/storefront/view-log` 返回成功而非 401
  - Admin/Sales 查询浏览日志
  - Sales 查操作日志/登录日志 返回 403

## 八、前端商品浏览上报

- [ ] 8.1 改造 `logging.ts`  
  文件：`frontend/next-app/src/lib/storefront/logging.ts`  
  - 保留原有 `recordBrowseEvent()` 与 CustomEvent 类型定义
  - 新增 `getOrCreateAnonymousId()`
  - 新增 `reportViewLog(payload)` → `fetch("POST /api/storefront/view-log", { productId, categoryId, anonymousId })`
  - 若匿名标识生成失败，则跳过浏览上报

- [ ] 8.2 改造 `product-view-beacon.tsx`  
  文件：`frontend/next-app/src/features/storefront/catalog/product-view-beacon.tsx`  
  - `useEffect` 中调用 `reportViewLog()` 替代 `recordBrowseEvent()`
  - 仅商品详情页改为直连后端上报
  - 不改造 `browse-event-link.tsx`、`search-form.tsx`

## 九、前端日志查询

- [ ] 9.1 新增类型定义  
  文件：`frontend/next-app/src/lib/admin/types.ts`  
  - `LoginLogEntry`、`OperationLogEntry`、`ProductViewLogEntry`、对应的 ListResult

- [ ] 9.2 新增客户端查询函数  
  文件：`frontend/next-app/src/lib/admin/client.ts`  
  - `getLoginLogs(params?)`、`getOperationLogs(params?)`、`getViewLogs(params?)`

- [ ] 9.3 新增服务端查询函数  
  文件：`frontend/next-app/src/lib/admin/server.ts`  
  - `getServerLoginLogs(params?)`、`getServerOperationLogs(params?)`、`getServerViewLogs(params?)`

## 完成标准

- 登录成功/失败自动写入数据库
- Admin 写操作自动写入操作日志（AOP 注解驱动）
- 业务方法异常不被操作日志切面吞掉
- 用户浏览商品详情自动上报到数据库
- 匿名用户可访问 `/api/storefront/view-log` 并写入 `anonymous_id`
- 操作日志写入失败不阻断主业务
- 登录日志写入失败不改变原始登录结果
- Admin 可查询所有三类日志
- Sales 可查询浏览日志，不可查询登录日志和操作日志

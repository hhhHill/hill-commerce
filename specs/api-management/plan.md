# API 管理规范化 实施计划

**Feature**: `api-management`  
**Status**: active  
**Date**: 2026-05-28  
**Spec**: `specs/api-management/spec.md`  

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

## Summary

本功能分三条主线交付：后端接入 Spring Boot 4 兼容的 springdoc-openapi 3.x，开发环境开放 Swagger UI/OpenAPI JSON 且生产环境关闭；后端安全异常统一返回 `code/message`；前端建立统一 HTTP client，把浏览器端请求、Server Component 直连后端请求和 BFF 代理职责拆清楚，并用 `ApiError` 保留后端业务错误码。

## Technical Context

**Language/Version**: Java 21, TypeScript 5.8 strict  
**Primary Dependencies**: Spring Boot 4.0.x, Spring Security 7, springdoc-openapi 3.0.3, Next.js 15.5, React 19, Vitest  
**Storage**: N/A，本功能不变更数据库 schema  
**Testing**: JUnit 5 + Spring Boot Test + MockMvc, Vitest + jsdom  
**Target Platform**: 后端 Spring MVC API, 前端 Next.js App Router  
**Project Type**: 前后端分离 Web application  
**Performance Goals**: HTTP client 抽象不增加额外网络跳转；OpenAPI 仅开发环境使用，不影响生产主路径  
**Constraints**: 后端保持认证事实源；首版不依赖 Redis/RocketMQ；API 基础路径保持 `/api/`；生产环境不得暴露 Swagger UI/OpenAPI JSON  
**Scale/Scope**: 覆盖后端全量 `@RestController` 文档生成，前端至少覆盖 spec 中列出的 domain client/server fetch wrapper  

## Constitution Check

| Gate | Status | Notes |
|------|--------|-------|
| Canonical Spec First | PASS | 行为变更已由 `specs/api-management/spec.md` 定义 |
| No Dual Truth | PASS | Apifox 仅作为 OpenAPI 导入消费者，不作为 API 真相来源 |
| Feature Specs Are Canonical | PASS | 本计划落在 `specs/api-management/` |
| Evidence Before Drift | PASS | 计划基于现有 `SecurityConfig`、`ApiExceptionHandler`、前端 fetch wrapper 事实 |
| Scaled Spec Governance | PASS | 涉及权限/鉴权与跨模块 HTTP 基础设施，已有 spec |
| Spec Lifecycle | PASS | spec 已标记 `Lifecycle: active` |
| Project Constraints | PASS | 不引入 Redis/RocketMQ，不改变 session cookie 认证，不改变 `/api/` 基础路径 |

无 constitution 例外项。

## Project Structure

### Documentation

```text
specs/api-management/
├── spec.md
├── plan.md
├── research.md          # 可选：记录版本和设计决策
├── data-model.md        # 可选：记录 ApiError / ApiErrorResponse 逻辑模型
├── quickstart.md        # 可选：记录验证步骤
├── contracts/           # 可选：放 OpenAPI 错误响应 contract 摘要
└── tasks.md             # 后续由 /speckit-tasks 生成
```

### Source Code

```text
backend/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/hillcommerce/framework/
    │   │   ├── security/SecurityConfig.java
    │   │   └── web/
    │   │       ├── ApiExceptionHandler.java
    │   │       └── ErrorCode.java
    │   └── resources/
    │       ├── application.yml
    │       └── application-prod.yml
    └── test/java/com/hillcommerce/

frontend/next-app/
└── src/lib/
    ├── http/
    │   ├── client.ts
    │   ├── errors.ts
    │   ├── server.ts
    │   └── types.ts
    ├── auth/
    ├── cart/
    ├── order/
    ├── payment/
    ├── storefront/
    └── admin/
```

**Structure Decision**: 后端只改 framework 层配置与安全错误输出，不进入业务模块；前端新增 `lib/http` 作为跨 domain 基础设施，各 domain 文件只替换内部 fetch wrapper，保持业务函数对外签名不变。

## Research Decisions

### R1: springdoc 版本

**Decision**: 使用 `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3`。  
**Rationale**: 项目使用 Spring Boot 4.0.x，springdoc 官方兼容矩阵要求 Spring Boot 4 对应 springdoc 3.0.x。  
**Alternatives Considered**: `2.8.x` 仅适配 Spring Boot 3.x，不用于本项目。

### R2: Swagger 端点暴露策略

**Decision**: dev profile 开放 `/swagger-ui.html`、`/swagger-ui/**`、`/v3/api-docs`、`/v3/api-docs/**`；prod profile 通过 springdoc 配置关闭。  
**Rationale**: 当前 `SecurityConfig` 默认认证所有未显式放行请求，不处理会导致文档不可访问；生产安全不能只依赖 Security permit 配置。  
**Alternatives Considered**: 生产环境鉴权后开放 Swagger。该方案增加运维风险，MVP 不需要。

### R3: 错误响应文档策略

**Decision**: 定义统一 `ApiErrorResponse` schema，并集中挂载常见错误响应；不逐个 Controller 添加业务错误码注解。  
**Rationale**: 后端业务错误由 `BusinessException/ErrorCode` 运行时决定，springdoc 无法可靠自动推断每个接口所有错误码。集中 schema 能满足前端和 Apifox 的基础契约。  
**Alternatives Considered**: 批量添加 `@Operation`/`@ApiResponse`。成本高且容易与代码漂移。

### R4: 前端 client 分层

**Decision**: `client.ts` 给浏览器/Client Component 使用，`server.ts` 使用 `server-only` 并负责 cookie 转发和后端直连。  
**Rationale**: Next.js Server Component 可以读取 `cookies()`，Client Component 不能；混用会导致 bundle 边界和认证语义不清。  
**Alternatives Considered**: 单文件同时导出 `api` 和 `backendApi`。容易被客户端误导入 server-only 逻辑。

## Design

### Backend OpenAPI

- 在 `backend/pom.xml` 增加 springdoc 3.0.3 依赖。
- 在 `application.yml` 增加 `springdoc.api-docs.path=/v3/api-docs` 和 Swagger UI path/sorter 配置。
- 在 `application-prod.yml` 关闭 `springdoc.api-docs.enabled` 和 `springdoc.swagger-ui.enabled`。
- 在安全配置中为非生产环境放行 Swagger/OpenAPI 端点。
- 增加集中式 OpenAPI 配置时，优先放在 framework/web 或 framework/config 下，不分散进业务 Controller。

### Backend Error Contract

- 保持 `ApiExceptionHandler` 对 `BusinessException` 的 `code/message` 响应格式。
- 修改 Spring Security 401/403 输出：
  - 401 使用 `ErrorCode.AUTHENTICATION_REQUIRED.code()`。
  - 403 使用 `ErrorCode.ACCESS_DENIED.code()`。
- 响应 content type 保持 `application/json`。
- 不改变 session、角色、`@RequireRole` 或 `@PreAuthorize` 的认证事实来源。

### Frontend HTTP Core

- `errors.ts`
  - 定义 `ApiError`。
  - 定义 `parseApiError(response)` 或等价函数。
  - 处理 JSON、非 JSON、缺少 code、204 等分支。
- `types.ts`
  - 定义 `ApiErrorPayload`、HTTP method、request options 等通用类型。
  - 不放任何业务 DTO。
- `client.ts`
  - 面向浏览器，默认相对请求 `/api/**`。
  - 默认 `credentials: "include"`。
  - JSON body 自动 stringify；`FormData` 不设置 content type。
- `server.ts`
  - 首行引入 `server-only`。
  - 使用 `getBackendBaseUrl()` 和 `cookies()`。
  - 默认 `cache: "no-store"`、`redirect: "manual"`。

### Domain Wrapper Migration

按风险从低到高迁移：

1. 纯公开读取：`storefront/client.ts`。
2. 用户态浏览器请求：`auth/client.ts`、`cart/client.ts`、`order/client.ts`、`payment/client.ts`。
3. 管理后台浏览器请求：`admin/client.ts`、`admin/analytics-client.ts`、`admin/homepage.ts`。
4. Server Component 读取：`admin/server.ts`、`cart/server.ts`、`order/server.ts`、`payment/server.ts`、`storefront/server.ts`。

迁移规则：

- 保留所有 exported business function 的名称、参数、返回值。
- 删除 domain 内重复 `safeJson()`、`sendXxxRequest()`、`fetchXxxJson()`。
- 删除或停止使用只包装 HTTP status/message 的 domain request error class。
- 保留 query builder 和业务参数拼装。

## Contracts

### Error Response

```json
{
  "code": 2001,
  "message": "用户不存在"
}
```

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `code` | integer | yes | 后端 `ErrorCode.code()` |
| `message` | string | yes | 用户可读或调试可读错误说明 |

### Frontend ApiError

```ts
class ApiError extends Error {
  readonly code: number;
  readonly httpStatus: number;
  readonly message: string;
}
```

### OpenAPI Endpoints

| Endpoint | Environment | Expected |
|----------|-------------|----------|
| `/swagger-ui.html` | dev | Swagger UI 可访问 |
| `/swagger-ui/**` | dev | Swagger UI 静态资源可访问 |
| `/v3/api-docs` | dev | OpenAPI JSON 可访问 |
| `/v3/api-docs/**` | dev | OpenAPI 分组资源可访问 |
| `/swagger-ui.html` | prod | 不暴露文档能力 |
| `/v3/api-docs` | prod | 不暴露文档能力 |

## Implementation Phases

### Phase 1: 后端 OpenAPI

- [ ] 在 `backend/pom.xml` 增加 springdoc 3.0.3。
- [ ] 在 `application.yml` 增加 springdoc dev 配置。
- [ ] 在 `application-prod.yml` 关闭 springdoc。
- [ ] 调整安全配置，使文档端点仅非生产环境可访问。
- [ ] 如需统一错误响应 schema，新增集中式 OpenAPI 配置。
- [ ] 验证 `/swagger-ui.html` 和 `/v3/api-docs`。

### Phase 2: 后端安全错误响应

- [ ] 修改 401 `AuthenticationEntryPoint` 输出 `code/message`。
- [ ] 修改 403 `AccessDeniedHandler` 输出 `code/message`。
- [ ] 增加或更新 MockMvc 测试覆盖未登录和权限不足场景。
- [ ] 确认现有业务异常响应未被破坏。

### Phase 3: 前端 HTTP 基础设施

- [ ] 新增 `frontend/next-app/src/lib/http/errors.ts`。
- [ ] 新增 `frontend/next-app/src/lib/http/types.ts`。
- [ ] 新增 `frontend/next-app/src/lib/http/client.ts`。
- [ ] 新增 `frontend/next-app/src/lib/http/server.ts`。
- [ ] 为 `ApiError` 解析、204、非 JSON 错误体、FormData 增加单元测试。

### Phase 4: 前端 Domain Wrapper 迁移

- [ ] 迁移 `storefront/client.ts`。
- [ ] 迁移 `auth/client.ts`。
- [ ] 迁移 `cart/client.ts`。
- [ ] 迁移 `order/client.ts`。
- [ ] 迁移 `payment/client.ts`。
- [ ] 迁移 `admin/client.ts`、`admin/analytics-client.ts`、`admin/homepage.ts`。
- [ ] 迁移 server-side fetch wrapper 到 `backendApi`。
- [ ] 搜索确认不再新增重复 `safeJson()` / `sendXxxRequest()` / `fetchXxxJson()`。

### Phase 5: 回归验证

- [ ] 后端运行 `mvn test` 或至少运行安全/OpenAPI 相关测试。
- [ ] 前端运行 `npm test`。
- [ ] 手动验证登录、购物车、下单、支付、管理后台 CRUD。
- [ ] 手动触发业务错误，确认 `ApiError.code/httpStatus/message` 可见。
- [ ] 确认 Client Component 没有导入 `@/lib/http/server`。

## Testing Strategy

### Backend

| Test | Purpose |
|------|---------|
| Swagger dev access test | `/swagger-ui.html` 和 `/v3/api-docs` 在 dev 下可访问 |
| Springdoc prod disabled test | prod 下文档端点不可用 |
| Security 401 test | 未登录访问受保护 API 返回 `AUTHENTICATION_REQUIRED` |
| Security 403 test | 权限不足返回 `ACCESS_DENIED` |
| BusinessException regression | 业务异常仍返回原有 `ErrorCode.code()` |

### Frontend

| Test | Purpose |
|------|---------|
| ApiError JSON parse | 后端 `code/message` 转成 `ApiError` |
| ApiError fallback | 非 JSON 或缺少 code 时按 HTTP status 兜底 |
| 204 response | 返回 `undefined` 且不解析 JSON |
| JSON request body | 自动设置 content type 并 stringify |
| FormData request body | 不手动设置 content type |
| server-only import boundary | server client 不被 Client Component 导入 |

## Risks And Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| springdoc 版本与 Boot 4 不兼容 | 后端启动失败 | 固定 3.0.3，并在后端编译/启动验证 |
| Swagger 端点被 Security 拦截 | 文档不可用 | dev profile 显式放行 `/swagger-ui/**` 和 `/v3/api-docs/**` |
| 生产误暴露文档 | 安全风险 | `application-prod.yml` 关闭 springdoc，不只依赖安全规则 |
| 前端 server/client 边界混用 | 构建失败或 cookie 丢失 | `server.ts` 使用 `server-only`，迁移时搜索导入 |
| 旧 domain error class 被组件依赖 | 行为回归 | 迁移前搜索 `instanceof CartRequestError/PaymentRequestError` 等调用点，逐个替换为 `ApiError` |
| 批量迁移范围大 | 回归风险 | 按 domain 分批迁移，每批运行相关测试和 smoke flow |

## Complexity Tracking

无 constitution 违反项，无额外复杂度豁免。

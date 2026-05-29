# API 管理规范化

**Lifecycle**: active

## 问题

1. **无 API 文档** — 后端未集成 OpenAPI/Swagger，接口发现、联调和 Apifox 导入都依赖人工查代码。
2. **前端 HTTP 封装严重重复** — 浏览器端、Server Component 端、管理后台、购物车、订单、支付、商品、认证等模块各自维护 fetch 包装、JSON 解析、错误处理和 cookie 转发逻辑。
3. **错误码被丢弃** — 后端业务异常返回 `{"code": 2001, "message": "..."}`，但前端多数调用只读取 `message` 或直接抛普通 `Error`。
4. **错误响应结构不完全统一** — `ApiExceptionHandler` 返回业务错误码，但 Spring Security 的 401/403 当前只返回 `{"message":"..."}`，无法与前端统一错误模型完全对齐。
5. **TypeScript 类型仍需手写维护** — 当前阶段不引入代码生成，因此本功能只解决 API 可见性和 HTTP 基础设施复用，不解决前后端 DTO 自动同步。

## 目标

- 开发环境启动后可访问可视化 API 文档和 OpenAPI JSON。
- Swagger/OpenAPI 端点在开发环境可用，在生产环境显式关闭。
- 前端请求基础能力收归到统一模块，新增 API 调用不再重复写 fetch 样板代码。
- 前端统一错误对象暴露 `code`、`httpStatus`、`message`，与后端 `ErrorCode` 体系对齐。
- 浏览器端请求、Server Component 直连后端请求、BFF 代理层职责边界清晰。
- 为后续 Apifox 接入保留稳定入口 `/v3/api-docs`。

## 成功标准

- 后端 dev profile 下访问 `/swagger-ui.html` 返回 Swagger UI，访问 `/v3/api-docs` 返回 OpenAPI JSON。
- 后端 prod profile 下 `/swagger-ui.html` 和 `/v3/api-docs` 不暴露文档能力。
- `/v3/api-docs` 至少包含所有 `@RestController` 暴露的 `/api/**` 路由。
- 前端业务函数的公开函数名、参数和返回值保持兼容，组件调用方不需要批量改造。
- 浏览器端和服务端 fetch 封装都能把后端 `code/message` 响应解析为 `ApiError`。
- 401/403 响应也具备统一错误结构，前端不需要对安全错误做特殊 JSON 分支。

## 方案概述

分三步独立交付：

| 步骤 | 改动范围 | 影响 |
|------|----------|------|
| 后端集成 springdoc-openapi | `pom.xml`、`application.yml`、`application-prod.yml`、`SecurityConfig` | 开发环境提供 Swagger UI 和 OpenAPI JSON |
| 后端统一安全错误响应 | `SecurityConfig` 或安全异常处理组件 | 401/403 返回 `code/message` |
| 前端统一 HTTP client | 新增 `lib/http/*`，替换各 domain wrapper 内部实现 | 业务函数签名不变，组件调用方保持不变 |

---

## 一、后端：springdoc-openapi

### 1.1 依赖

项目使用 Spring Boot 4.0.x，springdoc 必须使用 3.0.x 系列。当前固定版本：

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>3.0.3</version>
</dependency>
```

版本选择依据：springdoc 官方兼容矩阵中 Spring Boot `4.0.x` 对应 springdoc `3.0.x`，当前文档示例稳定版本为 `3.0.3`。

### 1.2 配置文件

项目使用 YAML 配置，新增配置写入 `backend/src/main/resources/application.yml`：

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method
    tags-sorter: alpha
```

生产环境写入 `backend/src/main/resources/application-prod.yml`：

```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

### 1.3 安全放行

当前 `SecurityConfig` 默认 `.anyRequest().authenticated()`，因此必须显式处理文档端点。

开发环境要求：

- `/swagger-ui.html` 可访问
- `/swagger-ui/**` 可访问
- `/v3/api-docs` 可访问
- `/v3/api-docs/**` 可访问

生产环境要求：

- 不依赖安全放行保护生产文档端点，必须通过 `application-prod.yml` 关闭 springdoc。
- 即使配置误放行，生产环境也不应生成 Swagger UI 或 OpenAPI JSON。

推荐实现方式：

- 用 `@Profile("!prod")` 或配置属性声明一个仅非生产环境生效的 SecurityFilterChain customizer/配置类。
- 不把 Swagger 端点混入业务公开 API 列表，避免把开发工具端点误当产品 API。

### 1.4 自动生成能力边界

springdoc 可自动识别：

- `@RestController` 和 `@RequestMapping` 路径
- HTTP 方法、请求参数、请求体、响应 DTO
- Java `record` 字段名和类型
- Bean Validation 注解，例如 `@NotNull`、`@Min`、`@Max`、`@Size`

springdoc 不应被假设能自动完成：

- 每个接口可能抛出的业务错误码清单
- `BusinessException` 与接口级错误响应的精确映射
- 所有安全错误、限流错误、校验错误的语义描述

因此本阶段只要求 OpenAPI 中有统一错误响应 schema，不要求每个接口逐一列出全部业务错误码。

### 1.5 统一错误响应文档

OpenAPI 中必须有统一错误响应结构：

```json
{
  "code": 2001,
  "message": "用户不存在"
}
```

要求：

- 定义 `ApiErrorResponse` schema，字段包含 `code: integer`、`message: string`。
- 全局或分组挂载 400、401、403、404、429、500 的错误响应说明。
- 不强制在每个 Controller 上加 `@Operation` / `@Schema` 描述注解。
- 若需要补足全局错误响应，优先使用集中式 OpenAPI 配置，而不是分散修改 Controller。

### 1.6 对外地址

| 地址 | 用途 |
|------|------|
| `/swagger-ui.html` | 开发时浏览器打开，可视化文档 + 在线调试 |
| `/v3/api-docs` | 机器可读 OpenAPI JSON，Apifox 导入入口 |

### 1.7 不做什么

- 不引入 API 版本化机制。
- 不为所有 Controller 批量添加描述性注解。
- 不把 Apifox 作为 API 真相来源。
- 不在生产环境暴露 Swagger UI 或 OpenAPI JSON。

---

## 二、后端：统一安全错误响应

### 2.1 目标响应格式

所有后端错误响应都应符合：

```json
{
  "code": 8001,
  "message": "forbidden"
}
```

### 2.2 安全异常映射

| 场景 | HTTP 状态 | ErrorCode | message |
|------|-----------|-----------|---------|
| 未登录访问受保护接口 | 401 | `AUTHENTICATION_REQUIRED` | `unauthorized` |
| 已登录但权限不足 | 403 | `ACCESS_DENIED` | `forbidden` |

### 2.3 实现约束

- 不引入前端自定义认证真相，后端仍是认证事实源。
- 不改变现有 session cookie 认证模式。
- 不改变业务 Controller 的鉴权语义。

---

## 三、前端：统一 HTTP Client

### 3.1 新增文件

```text
frontend/next-app/src/lib/http/errors.ts
frontend/next-app/src/lib/http/types.ts
frontend/next-app/src/lib/http/client.ts
frontend/next-app/src/lib/http/server.ts
```

职责：

| 文件 | 职责 |
|------|------|
| `errors.ts` | `ApiError` class、错误响应解析 |
| `types.ts` | 通用 HTTP 类型，不放业务 DTO |
| `client.ts` | 浏览器/Client Component 使用，允许 `"use client"` 导入 |
| `server.ts` | Server Component/服务端函数使用，必须 `import "server-only"` |

### 3.2 ApiError

```ts
export class ApiError extends Error {
  constructor(
    public readonly code: number,
    public readonly httpStatus: number,
    message: string
  ) {
    super(message);
    this.name = "ApiError";
  }
}
```

解析规则：

- 后端返回 `{"code": 2001, "message": "用户不存在"}` 时，抛出 `new ApiError(2001, 404, "用户不存在")`。
- 如果响应体不是 JSON 或缺少 `code`，使用 HTTP 状态映射兜底：
  - 400 → `BAD_REQUEST`
  - 401 → `AUTHENTICATION_REQUIRED`
  - 403 → `ACCESS_DENIED`
  - 404 → `NOT_FOUND`
  - 429 → `RATE_LIMIT_EXCEEDED`
  - 其他 5xx → `INTERNAL_ERROR`
- 兜底 message 使用后端 message、HTTP status text 或 `"请求失败，请稍后重试"`。

### 3.3 浏览器端 API

```ts
import { api } from "@/lib/http/client";

const cart = await api.get<Cart>("/api/cart");
const result = await api.post<CartMutation>("/api/cart", { skuId: 1, quantity: 2 });
await api.put<CartMutation>("/api/cart/1", { quantity: 3 });
await api.delete("/api/cart/1");
```

默认行为：

- `credentials: "include"`
- 有 JSON body 时设置 `Content-Type: application/json`
- `FormData` body 不手动设置 `Content-Type`
- 非 ok 响应自动抛 `ApiError`
- 204 返回 `undefined`
- 默认请求相对路径 `/api/...`，继续经过 Next.js BFF route handler

### 3.4 服务端 API

```ts
import { backendApi } from "@/lib/http/server";

const data = await backendApi.get<T>("/api/admin/products");
```

要求：

- `server.ts` 必须 `import "server-only"`，禁止被 Client Component 导入。
- 通过 `getBackendBaseUrl()` 直连后端。
- 使用 `next/headers` 的 `cookies()` 生成 cookie header 并转发。
- 默认 `cache: "no-store"`、`redirect: "manual"`。
- 非 ok 响应自动抛 `ApiError`。

命名：

- `api`：浏览器端相对请求，走 BFF。
- `backendApi`：服务端直连后端。
- 如需要管理后台语义别名，可以导出 `adminApi = backendApi`，但实现必须复用同一 server-only client。

### 3.5 BFF 代理层

`frontend/next-app/src/lib/auth/proxy.ts` 的 `proxyBackendRequest` 继续保留。

约束：

- BFF 代理负责 route handler 到后端的透传、cookie/set-cookie 处理。
- 统一 HTTP client 不取代 BFF 代理层。
- 浏览器端 `api` 请求仍访问 Next.js `/api/**` route handler。
- Server Component 需要直连后端时使用 `backendApi`。

### 3.6 现有文件改造范围

至少覆盖以下重复封装：

| 类型 | 文件 |
|------|------|
| 浏览器端业务 client | `auth/client.ts`、`cart/client.ts`、`order/client.ts`、`payment/client.ts`、`storefront/client.ts` |
| 管理后台 client | `admin/client.ts`、`admin/analytics-client.ts`、`admin/homepage.ts` |
| 服务端读取 | `admin/server.ts`、`cart/server.ts`、`order/server.ts`、`payment/server.ts`、`storefront/server.ts` |

允许保留 domain-specific query builder，例如订单筛选、商品列表筛选等业务查询参数拼装。禁止继续保留重复的：

- `safeJson()`
- `sendXxxRequest()`
- `fetchXxxJson()`
- 只包装 HTTP status/message 的 domain request error class

### 3.7 不影响什么

- 业务函数名、参数、返回值保持不变。
- `storefront/types.ts`、`cart/types.ts`、`order/types.ts`、`payment/types.ts`、`admin/types.ts` 等业务 DTO 类型不在本阶段改造。
- 不引入 `openapi-typescript` 或其他类型生成。

---

## 四、Apifox 接入（预留）

不做任何以 Apifox 为源的配置。后续需要时：

1. 确保后端以开发配置运行。
2. 访问 `http://localhost:8080/v3/api-docs` 获取 OpenAPI JSON。
3. Apifox → 导入 → OpenAPI/Swagger → 粘贴上述 URL。
4. Apifox 自动生成文档、调试环境、Mock 规则。

后端已有的 `ApifoxUploaderProjectSetting.xml`（IDEA 插件配置）保留不动。

---

## 五、测试与验证

### 后端

- 启动 dev profile 后端，访问 `http://localhost:8080/swagger-ui.html`，确认页面可打开。
- 访问 `http://localhost:8080/v3/api-docs`，确认返回 OpenAPI JSON。
- 在 Swagger UI 中执行一个公开 GET 请求，确认在线调试可用。
- 对受保护 API 未登录请求返回 401，响应体包含 `code` 和 `message`。
- 对权限不足 API 请求返回 403，响应体包含 `code` 和 `message`。
- 启动 prod profile 或加载 `application-prod.yml`，确认 springdoc 文档端点被关闭。

### 前端

- 为 `ApiError` 解析增加单元测试，覆盖正常业务错误、401、403、非 JSON 错误体、204 响应。
- 跑现有测试 `npm test`（Vitest），确认无 regression。
- 浏览器操作登录、购物车、下单、支付、管理后台 CRUD，确认功能正常。
- 故意触发一个错误（如查询不存在商品或未登录访问购物车），确认抛出的 `ApiError` 包含 `code`、`httpStatus`、`message`。
- 确认 Client Component 没有导入 `@/lib/http/server`。

---

## 六、范围外

- 不引入 TypeScript 类型代码生成。
- 不新增 API 版本化机制。
- 不把 Apifox 作为 API 设计源或产品事实源。
- 不修改 BFF route handler 的路径结构。
- 不批量给 Controller 添加描述性 Swagger 注解。
- 不改变 session cookie 认证模式。

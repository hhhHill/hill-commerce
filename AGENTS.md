# Hill Commerce 电商平台 编码规范

Spring Boot 4.0 + Java 21 + MyBatis-Plus + Next.js 15 + React 19 电商平台。写代码时必须遵守以下规则。

---

## 一、项目结构

```
hill-commerce/
├── backend/                          # Spring Boot 后端
│   └── src/main/java/com/hillcommerce/
│       ├── HillCommerceApplication.java
│       ├── framework/                # 横切基础能力
│       │   ├── cache/                #   CacheService 接口 + NoopCacheService
│       │   ├── events/               #   DomainEvent / DomainEventPublisher
│       │   ├── security/             #   SecurityConfig
│       │   └── web/                  #   ApiExceptionHandler / HealthController
│       └── modules/                  # 业务模块
│           ├── admin/                #   管理后台
│           ├── cart/                 #   购物车
│           ├── common/               #   共享工具（BusinessIdGenerator）
│           ├── logging/              #   操作日志
│           ├── order/                #   订单中心
│           ├── oss/                  #   文件上传
│           ├── payment/              #   支付
│           ├── product/              #   商品目录
│           ├── recommendation/       #   Gorse 推荐
│           └── user/                 #   用户与认证
├── frontend/next-app/                # Next.js 前端
│   └── src/
│       ├── app/                      #   App Router 页面
│       ├── components/               #   共享组件
│       ├── features/                 #   按功能组织组件
│       └── lib/                      #   工具库（auth/cart/order/storefront/...）
├── specs/                            # 功能规范文档（17 个功能）
├── ops/                              # 运维配置（MySQL/Gorse/Nginx）
└── .specify/                         # Spec Kit 治理
```

**技术栈**：Spring Boot 4.0 / Java 21 / MyBatis-Plus 3.5 / Flyway / MySQL 9.7 / Spring Security 7（Lambda DSL）/ Next.js 15.5 / React 19 / TypeScript 5.8（strict）/ Tailwind CSS 4

---

## 二、后端分层架构

```
Controller → Service → Mapper (MyBatis-Plus BaseMapper)
                ↕
          Framework（CacheService、DomainEventPublisher）
```

### Controller 层

- 仅路由和委托，禁止业务逻辑
- `@RestController` + `@RequestMapping("/api/...")`，RESTful 风格
- 通过 `Authentication` 参数获取当前用户
- 返回 DTO，**禁止直接返回 Entity**
- 构造注入所有依赖

### Service 层

- `@Service` 注解，构造注入
- 业务逻辑编排，复杂 Service 按职责拆分
- 写操作加 `@Transactional`
- **禁止**在事务方法内调用外部 API（OSS 上传、邮件发送等）
- **禁止**同类内部调用 `@Transactional` 方法（AOP 代理不生效）

### Mapper 层

- 接口继承 MyBatis-Plus `BaseMapper<T>`，加 `@Mapper` 注解
- 自定义查询优先方法命名约定，复杂查询用 `@Select` / `@Update`
- 禁止在 Mapper 中写业务逻辑

---

## 三、命名与 Bean 约定

| 后缀 | 用途 | 示例 |
|------|------|------|
| `XxxEntity` | MyBatis-Plus 实体 | `ProductEntity`、`OrderEntity` |
| `XxxMapper` | 数据访问接口 | `ProductMapper` |
| `XxxService` | 业务服务 | `ProductService` |
| `XxxController` | REST 控制器 | `ProductController` |
| `XxxRequest` | 前端请求体（record） | `CreateOrderRequest` |
| `XxxResponse` | 前端响应体（record） | `OrderResponse` |

- DTO 优先用 Java `record`（不可变数据载体）
- Entity 使用 `@TableName` + `@TableId(type = IdType.AUTO)` 映射表
- 本模块内部用的 DTO 放在 Service 所在包的子包
- **构造注入**：禁止 `@Autowired` 字段注入

---

## 四、异常与错误处理

### 统一异常体系

使用 `BusinessException` 统一业务异常，配合 `ErrorCode` 枚举分域管理：

| 域 | 范围 | 示例 |
|----|------|------|
| 通用 | 1xxx | BAD_REQUEST(400)、NOT_FOUND(404) |
| 用户 | 2xxx | USER_NOT_FOUND(2001) |
| 商品 | 3xxx | PRODUCT_NOT_FOUND(3001) |
| 订单 | 4xxx | ORDER_NOT_FOUND(4001) |
| 购物车 | 5xxx | CART_ITEM_NOT_FOUND(5001) |
| 支付 | 6xxx | PAYMENT_FAILED(6001) |
| 文件 | 7xxx | UPLOAD_FAILED(7001) |
| 权限 | 8xxx | ACCESS_DENIED(8001) |

### 使用规则

- 抛出：`throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "商品不存在")`
- **禁止** `throw new RuntimeException(...)` 和裸 `IllegalArgumentException` —— 用 `BusinessException`
- 全局异常处理器 `ApiExceptionHandler`（`@RestControllerAdvice`）统一处理
- `catch (BusinessException e) { throw e; }` 保留业务异常原样抛出

---

## 五、数据库

- MySQL 9.7 + MyBatis-Plus 3.5
- 表结构迁移用 Flyway，文件命名：`V{序号}__{描述}.sql`
- 实体类 `@TableName` 指定表名，`@TableId(type = IdType.AUTO)` 自增主键
- `ddl-auto` 不使用（由 Flyway 管理）
- **禁止**在循环中调用 DB 查询，改用批量操作

### 事务规则

- `@Transactional` 放 Service 层写操作
- **禁止**事务内调用外部 API（OSS、邮件）
- **禁止**同类内部调用 `@Transactional` 方法
- 保持事务范围最小

---

## 六、安全

- 会话认证：Session cookie（JSESSIONID），`HttpSessionSecurityContextRepository`
- Spring Security 使用 Lambda DSL 配置
- CSRF 禁用（前后端分离），表单登录禁用
- 公开端点显式列在 `permitAll()` 中
- 权限校验用 `@PreAuthorize`，角色名用常量或枚举，不硬编码字符串
- **后端持有认证真相**，前端不得定义认证事实

---

## 七、配置管理

- 配置文件：`application.yml` + `.env`（`spring.config.import`）
- 敏感信息（数据库密码、OSS AK/SK、API Key）放 `.env`，不入版本控制
- 业务配置用 `@ConfigurationProperties`（配合 `@ConfigurationPropertiesScan`）
- **禁止** `@Value` 散落在 Service 中（集中到 Properties 类）

---

## 八、缓存与事件

### 缓存

- 通过 `CacheService` 接口编程，当前默认实现为 `NoopCacheService`
- MVP v1 **不依赖 Redis**，后续按需切换 `RedisCacheService`
- 调用方依赖接口，不依赖具体实现

### 领域事件

- 使用 `DomainEvent` 接口 + `DomainEventPublisher` 发布
- 当前为本地同步实现（`LocalDomainEventPublisher`）
- 事件类放在对应业务模块的 `events/` 子包
- MVP v1 **不依赖 RocketMQ**，后续可按需升级为异步消息

---

## 九、Spring Boot 基础规范

- 构造注入（禁止 `@Autowired` 字段注入）
- 使用 `@Configuration` + `@Bean` 声明式配置
- 优先用现代 Java 特性：`switch` 表达式、`instanceof` pattern matching、text blocks、`Optional`
- MyBatis-Plus 分页用 `Page<T>` + `PageQuery`

---

## 十、格式与命名

### Java

- 类名 UpperCamelCase，方法/字段 lowerCamelCase，常量 UPPER_SNAKE_CASE
- 包名全小写，按功能分层
- **禁止**通配符导入（`import java.util.*`）
- 禁止内联全限定类名（用 import 代替）
- 列宽建议 120 字符以内

### TypeScript / React

- 组件文件用 kebab-case（`product-card.tsx`），工具模块用 kebab-case
- 类型/接口名 UpperCamelCase，变量/函数 lowerCamelCase
- 组件用箭头函数 + 显式类型 props
- `@/` 路径别名映射 `./src/`
- **禁止** `any`（用 `unknown` 或具体类型）
- Server Component 为默认，"use client" 仅用于交互组件
- 客户端数据获取统一 `credentials: "include"` 携带会话 cookie
- Tailwind CSS 4，全局主题变量定义在 `globals.css`
- 领域库按 `lib/<domain>/{types,server,client,errors}.ts` 组织

---

## 十一、测试

### 后端

- JUnit 5 + Spring Boot Test + MockMvc + AssertJ
- 集成测试用 Testcontainers（MySQL），`@DynamicPropertySource` 注入连接
- `@BeforeEach` 用定向 DELETE 清理，**禁止全表清空**
- 测试文件放在 `src/test/java/com/hillcommerce/` 对应路径

### 前端

- Vitest + Testing Library React + jsdom
- 测试文件与源文件同目录：`*.test.ts` / `*.test.tsx`
- 组件测试关注用户行为，不测试实现细节

---

## 十二、AI 辅助编码指引

### 开始功能开发前

1. 读取 `specs/<feature>/spec.md` 获取功能需求（优先最具体的）
2. 读取 `specs/<feature>/plan.md` 获取实现方案
3. 读取 `specs/<feature>/tasks.md` 获取任务拆分
4. 读取 `.specify/memory/constitution.md` 获取项目级治理规则

### 工作流规则

- 中高复杂度工作必须先读 `.specify/memory/constitution.md`，再读功能 spec
- 默认新工作放在 `specs/*`（非 `docs/superpowers/specs/*`）
- 跨多个功能时，确认一个主要功能 spec，其余作为次要参考
- `README.md` 作为开发者入口文档，规范约束提取到 canonical spec
- **高风险变更必须有 spec**：支付、订单、库存、权限、DB 结构、状态机、金额、外部回调、并发/事务/幂等
- 声明行为变更或 spec 迁移完成前，运行验证并确保 canonical spec 与结果一致

### 项目硬性约束

- 前后端分离，后端持有认证真相
- MVP v1 主路径不依赖 Redis 和 RocketMQ
- API 基础路径 `/api/`

---

## 速查：禁止清单

| 禁止项 | 原因 |
|--------|------|
| `throw new RuntimeException(...)` | 绕过全局异常处理，用 `BusinessException` |
| 直接返回 Entity 给前端 | 暴露内部结构，始终用 DTO |
| `@Value` 散落在 Service 中 | 集中到 `@ConfigurationProperties` |
| `@Autowired` 字段注入 | 隐式依赖，用构造注入 |
| 内联全限定类名 | 用 import 代替 |
| 事务内调用外部 API（OSS、邮件） | 占用 DB 连接 |
| 同类内部调用 `@Transactional` | AOP 代理不生效 |
| `catch (Exception e) {}` 静默忽略 | 隐藏错误 |
| 循环中逐条查询 DB | 改用批量操作 |
| 全表清空测试数据 | 用定向 DELETE |
| 硬编码密钥/密码 | 安全风险 |
| TypeScript `any` | 类型安全丧失 |
| Java 通配符导入 | 命名污染 |
| Server Component 使用客户端 hook | 编译错误 |

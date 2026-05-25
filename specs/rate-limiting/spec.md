# 全局限流系统

**Status**: draft

## 概述

为 hill-commerce 建立双层限流防护体系：Nginx 层做 IP 级粗粒度限流挡住基础攻击流量，Spring Boot 层做业务语义限流实现精准控制。两层职责分明，不重复拦截。应用层采用 **Bucket4j + Redis 令牌桶**算法，通过自定义 `@RateLimit` 注解 + AOP 切面实现声明式配置，零侵入业务代码。

## 架构

```
客户端请求
  │
  ├─ 第一层：Nginx limit_req_zone（IP 级，粗粒度，兜底）
  │   - 全局 /api/ 30r/s，仅挡明显流量攻击
  │   - 超限返回 429（Nginx 默认 HTML）
  │
  ├─ 第二层：Spring Boot RateLimitAspect（业务语义，细粒度）
  │   - @RateLimit 注解声明限流策略，支持方法级和类级
  │   - Bucket4j 令牌桶 + Redis 分布式共享
  │   - 超限返回 429 + JSON 错误体（对齐项目 ErrorCode 体系）
  │
  └─ 业务 Controller
```

### 两层分工

- **Nginx**：仅做粗粒度兜底保护（单 IP 30r/s），不以业务语义区分接口。不参与登录/注册限流——那部分完全交给应用层做维度更丰富的控制。
- **Spring Boot**：做业务语义限流——登录按 IP + 账号双维度、下单按用户、管理后台按用户。Nginx 已兜底全局速率，应用层不再设"其余 /api/**"全局规则。

### 为什么选令牌桶而非滑动窗口

- **令牌桶允许突发**：用户快速连续点击商品列表是正常浏览行为，不应惩罚
- **滑动窗口严格平滑**：更适合 API 网关对机器调用方的场景，对 C 端用户过于严厉
- 令牌桶在"平均控速"和"容忍突发"之间平衡最好

### 为什么选 Bucket4j 而非 Sentinel/Resilience4j

- 项目已是 Spring Web（Tomcat）而非 WebFlux/Gateway 体系，Sentinel 偏重且面向网关场景
- Resilience4j 的 RateLimiter 模块是单机 JVM 级别，多实例部署时计数不共享
- Bucket4j 原生支持 Redis 后端，分布式场景开箱即用；API 简洁，与 Spring AOP 天然契合

## Nginx 层

### 修改文件

| 文件 | 变更 |
|------|------|
| `ops/nginx/default.conf` | 新增 `limit_req_zone` 和 `limit_req` 指令 |

### 配置

Nginx 层只做全局 API 粗粒度限流（30r/s），登录/注册限流交给应用层按 IP + 账号双维度控制。

```nginx
# 定义限流区域（10MB 共享内存，约 16 万 IP）
limit_req_zone $binary_remote_addr zone=api:10m rate=30r/s;

server {
    listen 80;
    server_name _;

    # 通用 API — 全局速率兜底
    location /api/ {
        limit_req zone=api burst=10 nodelay;
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        proxy_pass http://frontend:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

- `burst=10 nodelay`：允许最多 10 个请求的瞬时突发，超出立即拒绝
- 登录/注册不在 Nginx 层做限流——由应用层按 IP + 账号双维度限流，更精准也更安全

## Spring Boot 层

### 依赖

`backend/pom.xml` 新增：

```xml
<!-- Bucket4j 核心令牌桶算法 -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.10.1</version>
</dependency>

<!-- Bucket4j Redis 集成：具体 artifact 根据项目确定的 Redis 客户端选择
     - Lettuce: bucket4j-lettuce
     - Jedis: bucket4j-jedis
     - Redisson: bucket4j-redisson
     实现阶段先确定项目 Redis 客户端方案后再锁定 artifact -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-redis</artifactId>
    <version>8.10.1</version>
</dependency>

<!-- Redis 客户端（项目目前没有 Redis，需新增；选择 Redisson） -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.37.0</version>
</dependency>
```

> **注意**：`bucket4j-redis` 的 artifact 名称要与 Bucket4j 8.x 实际模块名对齐。8.x 对 Lettuce/Jedis/Redisson 的拆分不同，具体 artifact 由实现阶段根据选择的 Redis 客户端确认。

### 配置

`application.yml` 新增：

```yaml
hill:
  rate-limit:
    enabled: ${RATE_LIMIT_ENABLED:true}
    redis-prefix: hill:rate-limit:
```

`application-dev.yml` 可覆盖关闭方便本地调试：

```yaml
hill:
  rate-limit:
    enabled: false
```

### 新建文件

| 文件 | 职责 |
|------|------|
| `framework/ratelimit/RateLimit.java` | `@RateLimit` 注解定义，支持 `TYPE` + `METHOD` |
| `framework/ratelimit/RateLimitProperties.java` | `@ConfigurationProperties("hill.rate-limit")` |
| `framework/ratelimit/RateLimitConfig.java` | Bucket4j Redis 代理 Bean 装配 |
| `framework/ratelimit/RateLimitAspect.java` | AOP 环绕切面，解析注解 → 解析 clientIp → 构造 key → 尝试消费令牌 → 超限抛异常 |
| `framework/ratelimit/RateLimitExceededException.java` | 自定义异常，携带 `nanosToWait` |
| `framework/web/RateLimitExceptionHandler.java` | `@RestControllerAdvice` 全局异常处理，返回 429 JSON（对齐 ErrorCode 体系） |
| `framework/web/ErrorCode.java` | 新增 `RATE_LIMIT_EXCEEDED` 错误码 |
| `framework/ratelimit/ClientIpResolver.java` | 从 `X-Forwarded-For` / `X-Real-IP` 解析真实客户端 IP |

### @RateLimit 注解

```java
package com.hillcommerce.framework.ratelimit;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 令牌桶容量（允许的最大突发） */
    long capacity() default 30;

    /** 每次补充的令牌数 */
    long refillTokens() default 10;

    /** 补充间隔 */
    long refillPeriod() default 1;

    /** 补充间隔时间单位，默认秒 */
    TimeUnit refillUnit() default TimeUnit.SECONDS;

    /** Redis key 的 SpEL 表达式，可用变量：clientIp、authentication */
    String key();

    /** 超限时的提示信息 */
    String message() default "请求过于频繁，请稍后再试";
}
```

- `@Target({ElementType.METHOD, ElementType.TYPE})` 支持方法级和类级标注
- 类级注解为类内所有方法提供默认限流策略，方法级注解覆盖类级（方法优先）
- SpEL 变量：`clientIp`（由切面统一注入，解析自 X-Forwarded-For/X-Real-IP）、`authentication`（当前认证信息，可能为 null）

### 各接口注解声明

```java
// AuthController.java — 登录双维度限流

@PostMapping("/api/auth/login")
@RateLimit(key = "login:ip:#{clientIp}", capacity = 5, refillTokens = 5,
    refillPeriod = 60, message = "登录尝试过于频繁，请1分钟后再试")
@RateLimit(key = "login:account:#{#email}", capacity = 5, refillTokens = 5,
    refillPeriod = 60, message = "该账号登录尝试过于频繁，请1分钟后再试")
public AuthUserResponse login(@Valid @RequestBody LoginRequest request, ...) { ... }

@PostMapping("/api/auth/register")
@RateLimit(key = "register:#{clientIp}", capacity = 3, refillTokens = 3,
    refillPeriod = 60, message = "注册过于频繁，请稍后再试")
public ResponseEntity<AuthUserResponse> register(...) { ... }

// StorefrontProductController.java

@GetMapping("/api/products")
@RateLimit(key = "products:#{clientIp}", capacity = 60, refillTokens = 30, refillPeriod = 30)
public Result listProducts(...) { ... }

@GetMapping("/api/products/{id}")
@RateLimit(key = "product-detail:#{clientIp}", capacity = 90, refillTokens = 30, refillPeriod = 10)
public Result getProduct(...) { ... }

// SearchController.java

@GetMapping("/api/search")
@RateLimit(key = "search:#{clientIp}", capacity = 30, refillTokens = 15, refillPeriod = 30)
public Result search(...) { ... }

// OrderController.java — 按登录用户限流

@PostMapping("/api/orders")
@RateLimit(key = "order-create:user:#{#userId}", capacity = 10, refillTokens = 5,
    refillPeriod = 60)
public Result createOrder(...) { ... }

// 管理后台控制器基类 — 类级注解
// 注意：key 中使用 #{#userId}（用户 ID），不使用 authentication.name（用户名可变）

@RestController
@RequestMapping("/api/admin")
@RateLimit(key = "admin:user:#{#userId}", capacity = 100, refillTokens = 50, refillPeriod = 30)
public class AdminControllerBase { ... }
```

**登录限流说明**：登录按双维度限流——
- `login:ip:<clientIp>`：单 IP 每分钟 5 次，防单 IP 撞库
- `login:account:<email>`：单账号每分钟 5 次，防同一账号被分布式撞库
- 任一桶耗尽即返回 429

**key 变量说明**：
- `clientIp`：由切面统一解析，取 `X-Forwarded-For` 最左非内网 IP，回退到 `X-Real-IP`，再回退到 `request.getRemoteAddr()`
- `#email` / `#userId`：从方法参数中提取，由 SpEL 表达式指定。`#email` 来自 `LoginRequest.email`，`#userId` 来自 `Authentication.principal.id`
- 未登录时 `#userId` 为 null → key 解析失败时切面降级到 `clientIp`

### RateLimitAspect 核心逻辑

```java
@Aspect
@Component
public class RateLimitAspect {

    private final RateLimitProperties properties;
    private final ClientIpResolver clientIpResolver;
    private final ProxyManager proxyManager; // Bucket4j Redis ProxyManager
    // ...

    @Around("(@within(rateLimit) || @annotation(rateLimit)) && "
          + "!@annotation(com.hillcommerce.framework.ratelimit.SkipRateLimit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }

        // 合并类级和方法级注解，方法级覆盖类级
        RateLimit effective = resolveEffectiveRateLimit(joinPoint);
        if (effective == null) {
            return joinPoint.proceed();
        }

        String key = resolveKey(effective.key(), joinPoint);
        String redisKey = properties.getRedisPrefix() + key;

        // 构造令牌桶配置
        Duration refillDuration = Duration.ofMillis(
            effective.refillUnit().toMillis(effective.refillPeriod()));
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.classic(
                effective.capacity(),
                Refill.greedy(effective.refillTokens(), refillDuration)))
            .build();

        // 获取 Redis 分布式令牌桶并尝试消费
        BucketProxy bucket = proxyManager.builder()
            .build(redisKey.getBytes(StandardCharsets.UTF_8), () -> configuration);

        // 单次调用，不重复消费
        ConsumptionProbe probe;
        try {
            probe = bucket.tryConsumeAndReturnRemaining(1);
        } catch (Exception e) {
            // fail-open：Redis 异常时降级放行 + 记录日志和指标
            log.warn("Rate limit Redis operation failed, failing open: key={}", redisKey, e);
            meterRegistry.counter("rate_limit.redis_error").increment();
            return joinPoint.proceed();
        }

        if (probe.isConsumed()) {
            return joinPoint.proceed();
        }

        long nanosToWait = probe.getNanosToWaitForRefill();
        throw new RateLimitExceededException(effective.message(), nanosToWait);
    }
}
```

**关键设计决策**：

- **单次 `tryConsumeAndReturnRemaining`**：一次调用完成消费 + 获取等待时间，不先 `tryConsume` 再 probe，避免语义不干净和双重消费风险
- **Duration 用 `toMillis`**：避免 `TimeUnit` 到 `ChronoUnit` 的手动映射和兼容风险
- **类级 + 方法级注解合并**：`@Around("(@within(rateLimit) || @annotation(rateLimit))")` 同时匹配类级和方法级；方法级优先级高于类级
- **Key 解析失败降级**：SpEL 解析 `#userId` 时如果 authentication 为 null，降级到 `clientIp`，不抛异常
- **未登录统一处理**：匿名用户所有 key 都走 `clientIp`，不共享 `anonymousUser` 桶
- **Redis fail-open**：Redis 异常时放行请求，记录 warn 日志 + 打点 `rate_limit.redis_error.count`。对登录接口需额外谨慎——可在 `RateLimitConfig` 中为高风险接口配置独立开关

### ClientIpResolver

```java
package com.hillcommerce.framework.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    /**
     * 解析客户端真实 IP：
     * 1. X-Forwarded-For 最左非内网地址（信任 Nginx 设置的值）
     * 2. X-Real-IP（Nginx 直接设置）
     * 3. request.getRemoteAddr() 兜底
     */
    public String resolve(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // 取最左非内网 IP
            for (String ip : forwardedFor.split(",")) {
                String trimmed = ip.trim();
                if (!isPrivateAddress(trimmed)) {
                    return trimmed;
                }
            }
            // 全部是内网地址时取最左
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private boolean isPrivateAddress(String ip) {
        // 10.x, 172.16-31.x, 192.168.x, 127.x, 0:0:0:0:0:0:0:1
        return ip.startsWith("10.") || ip.startsWith("192.168.") || ip.startsWith("127.")
            || ip.startsWith("0:0:0:0:0:0:0:1") || ip.equals("::1")
            || (ip.startsWith("172.") && ip.split("\\.").length >= 2
                && Integer.parseInt(ip.split("\\.")[1]) >= 16
                && Integer.parseInt(ip.split("\\.")[1]) <= 31);
    }
}
```

### 429 响应格式

对齐项目现有 `ErrorCode` 体系。`ErrorCode.java` 新增：

```java
// ── 9xxx 限流 ────────────────────────────────────────────
RATE_LIMIT_EXCEEDED(9001, HttpStatus.TOO_MANY_REQUESTS);
```

`RateLimitExceptionHandler` 返回：

```java
@RestControllerAdvice
public class RateLimitExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(
            RateLimitExceededException ex, HttpServletResponse response) {
        long retrySeconds = TimeUnit.NANOSECONDS.toSeconds(ex.getNanosToWait()) + 1;
        response.setHeader("Retry-After", String.valueOf(retrySeconds));
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .body(Map.of(
                "code", ErrorCode.RATE_LIMIT_EXCEEDED.code(),
                "message", ex.getMessage(),
                "retryAfter", retrySeconds
            ));
    }
}
```

响应体格式：

```json
{
  "code": 9001,
  "message": "请求过于频繁，请稍后再试",
  "retryAfter": 12
}
```

- `code` + `message` 对齐现有 `{code, message}` 模式
- `retryAfter` 额外告知前端建议重试等待秒数
- HTTP 头部同时带 `Retry-After: 12`

## 限流策略汇总

| 接口 | 维度 | 容量 | 填充速率 | 等效限制 |
|------|------|------|----------|----------|
| `/api/auth/login` | IP | 5 | 5/60s | 5次/分钟/IP |
| `/api/auth/login` | 账号(email) | 5 | 5/60s | 5次/分钟/账号 |
| `/api/auth/register` | IP | 3 | 3/60s | 3次/分钟 |
| `/api/products` (列表) | IP | 60 | 30/30s | ~60次/分钟 |
| `/api/products/{id}` (详情) | IP | 90 | 30/10s | ~180次/分钟 |
| `/api/search` | IP | 30 | 15/30s | ~30次/分钟 |
| `/api/orders` (创建) | 用户ID | 10 | 5/60s | ~5次/分钟 |
| `/api/admin/**` | 用户ID | 100 | 50/30s | ~100次/分钟 |
| 其余 `/api/**` | — | — | — | 仅 Nginx 30r/s 兜底 |

- 商品详情比列表更宽松——正常用户浏览时逐一点开详情频率远高于翻页
- 下单按用户限（需登录），防止脚本刷单
- 管理后台较宽松，不影响运营操作；类级注解统一声明，继承到所有子类方法
- **"其余 /api/**"不设应用层限流**，由 Nginx 层 30r/s 兜底。后续如需对特定接口限流，单独加 `@RateLimit` 注解

## 安全考虑

- SpEL 表达式解析控制在白名单变量（`clientIp`、`authentication`），不暴露任意对象
- `clientIp` 由切面统一注入，不直接暴露 `HttpServletRequest` 到 SpEL，防止注解中错误使用 `request.remoteAddr`
- Redis key 统一加前缀 `hill:rate-limit:`，避免与业务缓存冲突
- `ClientIpResolver` 只信任来自 Nginx 的 `X-Forwarded-For`，取最左非内网 IP
- 限流关闭开关（`RATE_LIMIT_ENABLED=false`）支持紧急情况下全局关闭
- Bucket4j Redis 操作失败时降级放行（fail-open），但必须：
  - 记录 **warn** 级别日志，包含 Redis key 和异常信息
  - 打点指标 `rate_limit.redis_error.count` 供监控告警
  - 后续可考虑短时间熔断（如连续失败 N 次后一定时间内全放行并打 error 日志），避免每个请求都打 Redis 然后报错
  - 对登录接口需评估是否适合 fail-open（Redis 全挂时登录限流完全放开有撞库风险），可在配置中为高风险接口提供独立的 `failOpen` 开关

## 数据库

**不变。** 限流状态完全存储在 Redis 中，不涉及 MySQL。

## 测试策略

| 层级 | 内容 | 工具 |
|------|------|------|
| `RateLimit.java` | 注解元数据正确性（Retention、Target 含 TYPE + METHOD） | 编译期验证 |
| `RateLimitAspect.java` | SpEL key 解析、注解合并（类级+方法级）、令牌消费/拒绝、fail-open、关闭开关 | JUnit 5 + Mockito |
| `ClientIpResolver.java` | X-Forwarded-For 解析、私有地址过滤、回退链 | JUnit 5 |
| `RateLimitExceptionHandler.java` | 429 状态码、JSON body（含 code/message/retryAfter）、Retry-After 头 | MockMvc `@WebMvcTest` |
| `RateLimitConfig.java` | disabled 时切面短路放行 | JUnit 5 + Spring Test |
| Nginx 限流 | `limit_req` 生效性、429 状态码 | curl 压测 |

## 不做

- 动态调整限流参数（运行时改阈值）—— 需要的话后续加配置中心
- 图形化管理界面
- 按租户/API Key 的限流（当前无多租户场景）
- IP 黑名单/白名单
- 浏览器端 429 重试 UI 提示 —— 前端后续单独迭代
- 前端 Next.js middleware 层限流 —— 两层已经覆盖，再加一层增加复杂度无收益

## 验收标准

- `/api/auth/login` 同一 IP 连续调用 5 次成功，第 6 次返回 429 + JSON 错误体（含 `code: 9001`）
- `/api/auth/login` 同一账号连续调用 5 次成功，第 6 次返回 429（双维度任一触发即限流）
- `/api/auth/register` 连续调用 3 次成功，第 4 次返回 429
- `/api/products` 在令牌桶耗尽后返回 429，等待 refill 后恢复
- 设置 `RATE_LIMIT_ENABLED=false` 后所有接口不限流
- Nginx 层超过 30r/s 返回 429（无 JSON body，Nginx 默认 HTML）
- 多实例部署时限流计数跨实例共享（Redis）
- Bucket4j Redis 操作异常时请求正常放行不报错（fail-open），且日志中有 warn 级别记录

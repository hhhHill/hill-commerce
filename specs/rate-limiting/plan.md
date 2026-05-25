# 单实例应用层限流 + Nginx 入口限流 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 本计划完成 **Nginx 入口限流（全局限流）+ Spring Boot 单实例应用层限流**。多实例分布式全局限流（Redis）由后续任务实现。应用层使用 Bucket4j 令牌桶 + `RateLimitBucketProvider` 接口解耦具体实现。

**Architecture:**
- Nginx 层：单 zone 30r/s 全局兜底，入口级
- Spring Boot 层：`@RateLimit` 注解 → `RateLimitAspect`（依赖接口）→ `LocalRateLimitBucketProvider`（ConcurrentHashMap + Bucket4j LocalBucket，单实例内有效）
- 接口 `RateLimitBucketProvider` 保证了后续 Redis 实现零改动接入

**已知限制（本阶段）：**
- 应用层限流仅在单 JVM 实例内有效，多实例场景需 Redis provider（后续任务）
- `LocalRateLimitBucketProvider` 使用无 TTL 的 ConcurrentHashMap，高基数 IP 场景会内存增长
- 登录账号维度的 key 包含明文邮箱，存在隐私风险（后续可哈希处理）

**Tech Stack:** Spring Boot（项目现有版本）, Java 21, Bucket4j 8.11.0（`bucket4j_jdk17-core`）

---

### Task 1: 依赖与配置

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/resources/application-dev.yml`

- [ ] **Step 1: 添加 Bucket4j 核心依赖并启用参数名保留**

在 `backend/pom.xml` 中：

1. `<properties>` 内新增（如已存在则合并）：
```xml
<maven.compiler.parameters>true</maven.compiler.parameters>
```

2. `<dependencies>` 中 aspectjweaver 之后新增：
```xml
<!-- Bucket4j 令牌桶核心（单实例本地桶，不含 Redis 分布式模块） -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j_jdk17-core</artifactId>
    <version>8.11.0</version>
</dependency>
```

- [ ] **Step 2: 添加应用配置到 application.yml**

在 `backend/src/main/resources/application.yml` 末尾新增：

```yaml
hill:
  rate-limit:
    enabled: ${RATE_LIMIT_ENABLED:true}
```

- [ ] **Step 3: 添加开发环境覆盖到 application-dev.yml**

```yaml
hill:
  rate-limit:
    enabled: false
```

- [ ] **Step 4: 验证依赖解析与编译**

```powershell
cd backend; ./mvnw dependency:resolve -q; ./mvnw compile -q
```

预期：BUILD SUCCESS。

---

### Task 2: ErrorCode 扩展

**Files:**
- Modify: `backend/src/main/java/com/hillcommerce/framework/web/ErrorCode.java`

- [ ] **Step 1: 添加限流错误码**

在 `ErrorCode.java` 的枚举常量末尾（`ACCESS_DENIED` 之后、`;` 之前）新增：

```java
// ── 9xxx 限流 ────────────────────────────────────────────
RATE_LIMIT_EXCEEDED(9001, HttpStatus.TOO_MANY_REQUESTS);
```

> `RATE_LIMIT_UNAVAILABLE(9002, 503)` 留到 Redis 分布式任务再加。本阶段本地 provider 无网络依赖，不会出现限流系统自身不可用的情况。

- [ ] **Step 2: 验证编译**

```powershell
cd backend; ./mvnw compile -q
```

---

### Task 3: ClientIpResolver — 带可信代理约束的 IP 解析

**Files:**
- Create: `backend/src/main/java/com/hillcommerce/framework/ratelimit/ClientIpResolver.java`
- Create: `backend/src/test/java/com/hillcommerce/framework/ratelimit/ClientIpResolverTest.java`

- [ ] **Step 1: 编写测试**

创建 `backend/src/test/java/com/hillcommerce/framework/ratelimit/ClientIpResolverTest.java`：

```java
package com.hillcommerce.framework.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClientIpResolverTest {

    private ClientIpResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ClientIpResolver(Set.of(
            "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16", "127.0.0.0/8"));
    }

    @Test
    void shouldReturnPublicIpFromXForwardedForWhenRemoteAddrIsTrustedProxy() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 10.0.0.2, 192.168.1.1");

        String ip = resolver.resolve(request);
        assertEquals("203.0.113.5", ip);
    }

    @Test
    void shouldIgnoreXForwardedForWhenRemoteAddrIsUntrusted() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.99");
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");

        String ip = resolver.resolve(request);
        assertEquals("203.0.113.99", ip);
    }

    @Test
    void shouldFallbackToXRealIpWhenXForwardedForMissing() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.10");

        String ip = resolver.resolve(request);
        assertEquals("203.0.113.10", ip);
    }

    @Test
    void shouldFallbackToRemoteAddrWhenBothHeadersMissing() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);

        String ip = resolver.resolve(request);
        assertEquals("10.0.0.1", ip);
    }

    @Test
    void shouldFilterOutUnknownTokenInXForwardedFor() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown, 203.0.113.5");

        String ip = resolver.resolve(request);
        assertEquals("203.0.113.5", ip);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```powershell
cd backend; ./mvnw test -Dtest=ClientIpResolverTest -q
```

- [ ] **Step 3: 实现 ClientIpResolver**

创建 `backend/src/main/java/com/hillcommerce/framework/ratelimit/ClientIpResolver.java`：

```java
package com.hillcommerce.framework.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    private static final Logger log = LoggerFactory.getLogger(ClientIpResolver.class);

    private static final List<String> NON_ROUTABLE_TOKENS = List.of(
        "unknown", "localhost", "127.0.0.1", "::1");

    private final Set<String> trustedProxyCidrs;

    public ClientIpResolver() {
        this(Set.of("10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16", "127.0.0.0/8"));
    }

    ClientIpResolver(Set<String> trustedProxyCidrs) {
        this.trustedProxyCidrs = trustedProxyCidrs;
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        if (isTrustedProxy(remoteAddr)) {
            // X-Forwarded-For: 取第一个非私有、非保留的 IP
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                for (String token : forwardedFor.split(",")) {
                    String ip = token.trim();
                    if (isPlausibleClientIp(ip) && !isPrivateAddress(ip)) {
                        return ip;
                    }
                }
            }

            // X-Real-IP: 第二优先级
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                String ip = realIp.trim();
                if (isPlausibleClientIp(ip)) {
                    return ip;
                }
            }
        }

        return remoteAddr;
    }

    private boolean isTrustedProxy(String ip) {
        for (String cidr : trustedProxyCidrs) {
            if (ipInCidr(ip, cidr)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPlausibleClientIp(String ip) {
        for (String token : NON_ROUTABLE_TOKENS) {
            if (token.equalsIgnoreCase(ip)) {
                return false;
            }
        }
        return !ip.contains(" ") && !ip.contains("\t") && !ip.isEmpty();
    }

    private boolean isPrivateAddress(String ip) {
        // 快速检查常见私有段
        if (ip.startsWith("10.") || ip.startsWith("192.168.") || ip.startsWith("127.")) {
            return true;
        }
        if (ip.startsWith("172.")) {
            String[] parts = ip.split("\\.");
            if (parts.length >= 2) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    if (second >= 16 && second <= 31) {
                        return true;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }
        return false;
    }

    private boolean ipInCidr(String ip, String cidr) {
        int slash = cidr.indexOf('/');
        if (slash < 0) {
            return ip.equals(cidr);
        }
        String prefix = cidr.substring(0, slash);
        int maskLen;
        try {
            maskLen = Integer.parseInt(cidr.substring(slash + 1));
        } catch (NumberFormatException e) {
            return false;
        }

        long ipLong = ipv4ToLong(ip);
        long prefixLong = ipv4ToLong(prefix);
        if (ipLong < 0 || prefixLong < 0) {
            return false;
        }
        // 正确的网络掩码：32 位全 1 左移 (32 - maskLen)，掩掉右边
        long mask = maskLen == 0 ? 0 : (0xFFFFFFFFL << (32 - maskLen)) & 0xFFFFFFFFL;
        return (ipLong & mask) == (prefixLong & mask);
    }

    private long ipv4ToLong(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return -1;
        }
        try {
            long result = 0;
            for (int i = 0; i < 4; i++) {
                int octet = Integer.parseInt(parts[i]);
                if (octet < 0 || octet > 255) {
                    return -1;
                }
                result = (result << 8) | octet;
            }
            return result & 0xFFFFFFFFL;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```powershell
cd backend; ./mvnw test -Dtest=ClientIpResolverTest -q
```

预期：Tests run: 5, Failures: 0。

---

### Task 4: RateLimit 注解 + RateLimitProperties

**Files:**
- Create: `backend/src/main/java/com/hillcommerce/framework/ratelimit/RateLimit.java`
- Create: `backend/src/main/java/com/hillcommerce/framework/ratelimit/RateLimitProperties.java`

- [ ] **Step 1: 创建 @RateLimit 注解**

创建 `backend/src/main/java/com/hillcommerce/framework/ratelimit/RateLimit.java`：

```java
package com.hillcommerce.framework.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 令牌桶限流注解。支持 TYPE（类级默认）和 METHOD（方法级覆盖）。
 * 当方法级存在时完全覆盖类级注解（不合并）。
 * 支持 @Repeatable，同一方法可标注多个实现多维度限流。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(RateLimit.RateLimits.class)
public @interface RateLimit {

    /** 令牌桶容量（允许的最大突发） */
    long capacity() default 30;

    /** 每次补充的令牌数 */
    long refillTokens() default 10;

    /** 补充间隔 */
    long refillPeriod() default 1;

    /** 补充间隔时间单位，默认秒 */
    TimeUnit refillUnit() default TimeUnit.SECONDS;

    /**
     * 限流 bucket key 的 SpEL 模板表达式。
     * 可用变量：{@code #clientIp}, {@code #authentication},
     * {@code #userId}, {@code #principalKey}, 以及方法参数名
     */
    String key();

    /** 超限时的提示信息 */
    String message() default "请求过于频繁，请稍后再试";

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface RateLimits {
        RateLimit[] value();
    }
}
```

**SpEL 变量参考**（由 `RateLimitAspect` 注入 `StandardEvaluationContext`）：

| 变量 | 类型 | 值 |
|------|------|-----|
| `#clientIp` | String | ClientIpResolver 解析结果 |
| `#authentication` | Authentication | 当前认证（可为 null） |
| `#userId` | Long | Authentication.principal.id（未登录时为 null） |
| `#principalKey` | String | 已登录 → `user:<id>`，未登录 → `ip:<clientIp>` |
| `#<paramName>` | Object | 方法参数（通过 `ParameterNameDiscoverer` 发现参数名） |

**覆盖规则**：方法有任意 `@RateLimit` → 完全忽略类级 `@RateLimit`。不合并。如需类级兜底 + 方法级约束同时生效，需在方法上同时标注两者。

- [ ] **Step 2: 创建 RateLimitProperties**

创建 `backend/src/main/java/com/hillcommerce/framework/ratelimit/RateLimitProperties.java`：

```java
package com.hillcommerce.framework.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "hill.rate-limit")
public record RateLimitProperties(
    @DefaultValue("true") boolean enabled
) {}
```

- [ ] **Step 3: 验证编译**

```powershell
cd backend; ./mvnw compile -q
```

---

### Task 5: RateLimitExceededException

**Files:**
- Create: `backend/src/main/java/com/hillcommerce/framework/ratelimit/RateLimitExceededException.java`

- [ ] **Step 1: 创建异常类**

创建 `backend/src/main/java/com/hillcommerce/framework/ratelimit/RateLimitExceededException.java`：

```java
package com.hillcommerce.framework.ratelimit;

public class RateLimitExceededException extends RuntimeException {

    private final long nanosToWait;

    public RateLimitExceededException(String message, long nanosToWait) {
        super(message);
        this.nanosToWait = nanosToWait;
    }

    /** 等待 refill 的纳秒数 */
    public long getNanosToWait() {
        return nanosToWait;
    }
}
```

- [ ] **Step 2: 验证编译**

```powershell
cd backend; ./mvnw compile -q
```

---

### Task 6: RateLimitBucketProvider 接口 + LocalRateLimitBucketProvider

**Files:**
- Create: `backend/src/main/java/com/hillcommerce/framework/ratelimit/RateLimitBucketProvider.java`
- Create: `backend/src/main/java/com/hillcommerce/framework/ratelimit/LocalRateLimitBucketProvider.java`
- Create: `backend/src/test/java/com/hillcommerce/framework/ratelimit/LocalRateLimitBucketProviderTest.java`

设计决策：抽象出 `RateLimitBucketProvider` 接口，让 `RateLimitAspect` 不依赖 Bucket4j 内部 ProxyManager/Builder 泛型链。本地实现用 `ConcurrentHashMap<String, Bucket>` + Bucket4j 线程安全的 `LocalBucket`。

**已知限制**：ConcurrentHashMap 无 TTL，高基数 IP 场景会持续增长。后续 Redis provider 会解决此问题。当前实现加注释标记此限制。

- [ ] **Step 1: 创建接口**

创建 `backend/src/main/java/com/hillcommerce/framework/ratelimit/RateLimitBucketProvider.java`：

```java
package com.hillcommerce.framework.ratelimit;

import com.bucket4j.ConsumptionProbe;

public interface RateLimitBucketProvider {

    /**
     * 尝试消费指定 key 对应的桶中的 1 个令牌。
     * 同一 key 的配置在进程生命周期内应保持不变；
     * 如果两个不同接口共用同一 key 但配置不同，只有首次配置生效。
     *
     * @return ConsumptionProbe（isConsumed() 为 true 放行，否则检查 nanosToWaitForRefill）
     */
    ConsumptionProbe tryConsumeAndReturnRemaining(String key, RateLimit config);
}
```

- [ ] **Step 2: 编写测试**

创建 `backend/src/test/java/com/hillcommerce/framework/ratelimit/LocalRateLimitBucketProviderTest.java`：

```java
package com.hillcommerce.framework.ratelimit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bucket4j.ConsumptionProbe;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LocalRateLimitBucketProviderTest {

    private LocalRateLimitBucketProvider provider;

    @BeforeEach
    void setUp() {
        provider = new LocalRateLimitBucketProvider();
    }

    @Test
    void shouldConsumeUpToCapacityThenReject() {
        RateLimit config = config(3, 3, 60, TimeUnit.SECONDS);
        String key = "test-key";
        assertTrue(provider.tryConsumeAndReturnRemaining(key, config).isConsumed());
        assertTrue(provider.tryConsumeAndReturnRemaining(key, config).isConsumed());
        assertTrue(provider.tryConsumeAndReturnRemaining(key, config).isConsumed());
        assertFalse(provider.tryConsumeAndReturnRemaining(key, config).isConsumed());
    }

    @Test
    void shouldSeparateKeys() {
        RateLimit config = config(1, 1, 60, TimeUnit.SECONDS);
        assertTrue(provider.tryConsumeAndReturnRemaining("key-A", config).isConsumed());
        assertTrue(provider.tryConsumeAndReturnRemaining("key-B", config).isConsumed());
        assertFalse(provider.tryConsumeAndReturnRemaining("key-A", config).isConsumed());
    }

    @Test
    void shouldReturnPositiveNanosToWaitWhenRejected() {
        RateLimit config = config(1, 1, 60, TimeUnit.SECONDS);
        String key = "wait-key";
        provider.tryConsumeAndReturnRemaining(key, config);
        ConsumptionProbe probe = provider.tryConsumeAndReturnRemaining(key, config);
        assertFalse(probe.isConsumed());
        assertTrue(probe.getNanosToWaitForRefill() > 0);
    }

    @Test
    void shouldRefillOverTime() throws InterruptedException {
        RateLimit config = config(1, 1, 200, TimeUnit.MILLISECONDS);
        String key = "refill-key";
        assertTrue(provider.tryConsumeAndReturnRemaining(key, config).isConsumed());
        assertFalse(provider.tryConsumeAndReturnRemaining(key, config).isConsumed());
        Thread.sleep(250);
        assertTrue(provider.tryConsumeAndReturnRemaining(key, config).isConsumed());
    }

    static RateLimit config(long capacity, long refillTokens, long refillPeriod, TimeUnit unit) {
        return new RateLimit() {
            @Override public long capacity() { return capacity; }
            @Override public long refillTokens() { return refillTokens; }
            @Override public long refillPeriod() { return refillPeriod; }
            @Override public TimeUnit refillUnit() { return unit; }
            @Override public String key() { return ""; }
            @Override public String message() { return ""; }
            @Override public Class<RateLimit> annotationType() { return RateLimit.class; }
        };
    }
}
```

- [ ] **Step 3: 运行测试确认失败**

```powershell
cd backend; ./mvnw test -Dtest=LocalRateLimitBucketProviderTest -q
```

- [ ] **Step 4: 实现 LocalRateLimitBucketProvider**

创建 `backend/src/main/java/com/hillcommerce/framework/ratelimit/LocalRateLimitBucketProvider.java`：

```java
package com.hillcommerce.framework.ratelimit;

import com.bucket4j.Bandwidth;
import com.bucket4j.Bucket;
import com.bucket4j.ConsumptionProbe;
import com.bucket4j.Refill;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 本地 ConcurrentHashMap + Bucket4j LocalBucket 实现。
 *
 * 已知限制：
 * - 无 TTL/淘汰机制，高基数 IP 场景下内存持续增长
 * - 同一 key 首次创建后配置即固定
 * - 仅单 JVM 实例内有效
 *
 * Redis provider 会解决以上限制。
 */
@Component
public class LocalRateLimitBucketProvider implements RateLimitBucketProvider {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public ConsumptionProbe tryConsumeAndReturnRemaining(String key, RateLimit config) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(config));
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    private Bucket createBucket(RateLimit config) {
        Duration refillDuration = Duration.ofMillis(
            config.refillUnit().toMillis(config.refillPeriod()));
        return Bucket.builder()
            .addLimit(Bandwidth.classic(
                config.capacity(),
                Refill.greedy(config.refillTokens(), refillDuration)))
            .build();
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

```powershell
cd backend; ./mvnw test -Dtest=LocalRateLimitBucketProviderTest -q
```

预期：Tests run: 4, Failures: 0。

---

### Task 7: RateLimitAspect — 核心切面

**Files:**
- Create: `backend/src/main/java/com/hillcommerce/framework/ratelimit/RateLimitAspect.java`
- Create: `backend/src/test/java/com/hillcommerce/framework/ratelimit/RateLimitAspectTest.java`

切面依赖于 `RateLimitBucketProvider` 接口和 `ClientIpResolver`，不依赖 Bucket4j 内部 API。

- [ ] **Step 1: 编写切面测试**

创建 `backend/src/test/java/com/hillcommerce/framework/ratelimit/RateLimitAspectTest.java`：

注：测试用真实 `@RateLimit` 注解的方法 + mock `MethodSignature`（`org.aspectj.lang.reflect.MethodSignature`，不是 `java.lang.reflect.Method`）来触发切面的注解解析。

```java
package com.hillcommerce.framework.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class RateLimitAspectTest {

    // ── 带 @RateLimit 注解的真实类 ──

    static class LimitedTarget {
        @RateLimit(key = "test:#{#clientIp}", capacity = 1, refillTokens = 1,
            refillPeriod = 60, refillUnit = TimeUnit.SECONDS,
            message = "单维度超限")
        public String limitedMethod() {
            return "ok";
        }

        @RateLimit(key = "dim-a:#{#clientIp}", capacity = 1, refillTokens = 1,
            refillPeriod = 60, refillUnit = TimeUnit.SECONDS)
        @RateLimit(key = "dim-b:#{#clientIp}", capacity = 1, refillTokens = 1,
            refillPeriod = 60, refillUnit = TimeUnit.SECONDS)
        public String multiDimMethod() {
            return "ok";
        }

        public String notAnnotated() {
            return "ok";
        }
    }

    private RateLimitProperties properties;
    private ClientIpResolver clientIpResolver;
    private RateLimitBucketProvider bucketProvider;
    private RateLimitAspect aspect;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties(true);
        clientIpResolver = mock(ClientIpResolver.class);
        bucketProvider = mock(RateLimitBucketProvider.class);
        aspect = new RateLimitAspect(properties, clientIpResolver, bucketProvider);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldProceedWhenDisabled() throws Throwable {
        RateLimitProperties disabledProps = new RateLimitProperties(false);
        RateLimitAspect disabledAspect = new RateLimitAspect(disabledProps, clientIpResolver, bucketProvider);
        ProceedingJoinPoint joinPoint = mockJoinPoint(LimitedTarget.class, "limitedMethod");

        when(joinPoint.proceed()).thenReturn("OK");
        Object result = disabledAspect.around(joinPoint);
        assertEquals("OK", result);
    }

    @Test
    void shouldProceedWhenNoAnnotation() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint(LimitedTarget.class, "notAnnotated");
        setupRequest("203.0.113.5");

        when(joinPoint.proceed()).thenReturn("OK");
        Object result = aspect.around(joinPoint);
        assertEquals("OK", result);
    }

    @Test
    void shouldThrowRateLimitExceededWhenBucketRejects() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint(LimitedTarget.class, "limitedMethod");
        setupRequest("203.0.113.5");
        when(clientIpResolver.resolve(any())).thenReturn("203.0.113.5");

        ConsumptionProbe rejected = ConsumptionProbe.rejected(5_000_000_000L);
        when(bucketProvider.tryConsumeAndReturnRemaining(
            eq("test:203.0.113.5"), any(RateLimit.class)))
            .thenReturn(rejected);

        assertThrows(RateLimitExceededException.class,
            () -> aspect.around(joinPoint));
    }

    @Test
    void shouldProceedWhenBucketAccepts() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint(LimitedTarget.class, "limitedMethod");
        setupRequest("203.0.113.5");
        when(clientIpResolver.resolve(any())).thenReturn("203.0.113.5");

        ConsumptionProbe consumed = ConsumptionProbe.consumed(0);
        when(bucketProvider.tryConsumeAndReturnRemaining(
            eq("test:203.0.113.5"), any(RateLimit.class)))
            .thenReturn(consumed);
        when(joinPoint.proceed()).thenReturn("OK");

        Object result = aspect.around(joinPoint);
        assertEquals("OK", result);
    }

    @Test
    void shouldFailOpenWhenBucketProviderThrows() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint(LimitedTarget.class, "limitedMethod");
        setupRequest("203.0.113.5");
        when(clientIpResolver.resolve(any())).thenReturn("203.0.113.5");

        when(bucketProvider.tryConsumeAndReturnRemaining(any(), any(RateLimit.class)))
            .thenThrow(new RuntimeException("local error"));
        when(joinPoint.proceed()).thenReturn("OK");

        Object result = aspect.around(joinPoint);
        assertEquals("OK", result);
    }

    @Test
    void shouldCheckAllDimensionsForMultiAnnotatedMethod() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint(LimitedTarget.class, "multiDimMethod");
        setupRequest("203.0.113.5");
        when(clientIpResolver.resolve(any())).thenReturn("203.0.113.5");

        // dim-a 通过，dim-b 拒
        when(bucketProvider.tryConsumeAndReturnRemaining(
            eq("dim-a:203.0.113.5"), any(RateLimit.class)))
            .thenReturn(ConsumptionProbe.consumed(0));
        when(bucketProvider.tryConsumeAndReturnRemaining(
            eq("dim-b:203.0.113.5"), any(RateLimit.class)))
            .thenReturn(ConsumptionProbe.rejected(5_000_000_000L));

        assertThrows(RateLimitExceededException.class,
            () -> aspect.around(joinPoint));
    }

    // ── helpers ──

    private ProceedingJoinPoint mockJoinPoint(Class<?> targetClass, String methodName) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        try {
            java.lang.reflect.Method method = targetClass.getMethod(methodName);
            MethodSignature signature = mock(MethodSignature.class);
            when(signature.getMethod()).thenReturn(method);
            when(joinPoint.getSignature()).thenReturn(signature);
            when(joinPoint.getTarget()).thenReturn(targetClass.getDeclaredConstructor().newInstance());
            when(joinPoint.getArgs()).thenReturn(new Object[0]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return joinPoint;
    }

    private void setupRequest(String remoteAddr) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(remoteAddr);
        ServletRequestAttributes attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```powershell
cd backend; ./mvnw test -Dtest=RateLimitAspectTest -q
```

- [ ] **Step 3: 实现 RateLimitAspect**

创建 `backend/src/main/java/com/hillcommerce/framework/ratelimit/RateLimitAspect.java`：

```java
package com.hillcommerce.framework.ratelimit;

import com.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.hillcommerce.modules.user.security.AuthenticatedUserPrincipal;

@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);
    private static final Pattern SPEL_TEMPLATE = Pattern.compile("#\\{([^}]*)\\}");

    private final RateLimitProperties properties;
    private final ClientIpResolver clientIpResolver;
    private final RateLimitBucketProvider bucketProvider;
    private final ExpressionParser spelParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public RateLimitAspect(RateLimitProperties properties,
                           ClientIpResolver clientIpResolver,
                           RateLimitBucketProvider bucketProvider) {
        this.properties = properties;
        this.clientIpResolver = clientIpResolver;
        this.bucketProvider = bucketProvider;
    }

    @Around("(@within(com.hillcommerce.framework.ratelimit.RateLimit) || "
          + "@within(com.hillcommerce.framework.ratelimit.RateLimit.RateLimits) || "
          + "@annotation(com.hillcommerce.framework.ratelimit.RateLimit) || "
          + "@annotation(com.hillcommerce.framework.ratelimit.RateLimit.RateLimits))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!properties.enabled()) {
            return joinPoint.proceed();
        }

        RateLimit[] annotations = resolveAnnotations(joinPoint);
        if (annotations.length == 0) {
            return joinPoint.proceed();
        }

        String clientIp = resolveClientIp();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        EvaluationContext spelContext = buildSpelContext(joinPoint, clientIp, authentication);

        for (RateLimit rateLimit : annotations) {
            String key = resolveKey(rateLimit.key(), spelContext);
            if (key == null) {
                log.warn("Rate limit key resolved to null for expression '{}', skipping this bucket",
                    rateLimit.key());
                continue;
            }

            ConsumptionProbe probe;
            try {
                probe = bucketProvider.tryConsumeAndReturnRemaining(key, rateLimit);
            } catch (Exception e) {
                log.warn("Bucket provider failed for key={}, failing open", key, e);
                continue;
            }

            if (!probe.isConsumed()) {
                throw new RateLimitExceededException(
                    rateLimit.message(), probe.getNanosToWaitForRefill());
            }
        }

        return joinPoint.proceed();
    }

    // ── annotation resolution ──

    private RateLimit[] resolveAnnotations(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = AopUtils.getMostSpecificMethod(signature.getMethod(),
            joinPoint.getTarget().getClass());

        // findMergedRepeatableAnnotations 返回 Set<A>
        Set<RateLimit> methodAnnotations = AnnotatedElementUtils.findMergedRepeatableAnnotations(
            method, RateLimit.class);
        if (!methodAnnotations.isEmpty()) {
            return methodAnnotations.toArray(RateLimit[]::new);
        }

        Set<RateLimit> classAnnotations = AnnotatedElementUtils.findMergedRepeatableAnnotations(
            joinPoint.getTarget().getClass(), RateLimit.class);
        return classAnnotations.toArray(RateLimit[]::new);
    }

    // ── SpEL context ──

    private EvaluationContext buildSpelContext(ProceedingJoinPoint joinPoint,
                                               String clientIp, Authentication authentication) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        context.setVariable("clientIp", clientIp);
        context.setVariable("authentication", authentication);

        // principalKey: 已登录 → user:<id>, 未登录 → ip:<ip>
        if (authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
            context.setVariable("userId", principal.id());
            context.setVariable("principalKey", "user:" + principal.id());
        } else {
            context.setVariable("principalKey", "ip:" + clientIp);
        }

        // 方法参数注入（需要 -parameters 编译选项）
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = AopUtils.getMostSpecificMethod(signature.getMethod(),
            joinPoint.getTarget().getClass());
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
        Object[] args = joinPoint.getArgs();

        if (paramNames != null) {
            for (int i = 0; i < Math.min(paramNames.length, args.length); i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        return context;
    }

    // ── SpEL template resolution ──

    private String resolveKey(String keyTemplate, EvaluationContext context) {
        Matcher matcher = SPEL_TEMPLATE.matcher(keyTemplate);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String expression = matcher.group(1);
            Object value;
            try {
                Expression expr = spelParser.parseExpression(expression);
                value = expr.getValue(context);
            } catch (Exception e) {
                log.error("SpEL evaluation failed for '{}', cannot resolve key — "
                    + "this annotation will be skipped", expression, e);
                return null;
            }
            String replacement = value != null ? value.toString() : "null";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String resolveClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "unknown";
        }
        HttpServletRequest request = attrs.getRequest();
        return clientIpResolver.resolve(request);
    }
}
```

关键设计：
- **切点匹配容器**：`@annotation(RateLimit.RateLimits)` + `@within(RateLimit.RateLimits)` 确保 Repeatable 不遗漏
- **SpEL 解析失败返回 null** → 外层 `continue` 跳过该注解，不 fallback 到 "unknown" 共享桶
- **`#principalKey`**：自动计算 `user:<id>` 或 `ip:<ip>`，避免 `#userId` 为 null 时的共享桶问题
- **`AopUtils`** 正确路径 `org.springframework.aop.support.AopUtils`
- **`findMergedRepeatableAnnotations`** 返回 `Set<RateLimit>`，使用 `.toArray(RateLimit[]::new)`

- [ ] **Step 4: 运行测试**

```powershell
cd backend; ./mvnw test -Dtest=RateLimitAspectTest -q
```

预期：Tests run: 6, Failures: 0。

---

### Task 8: RateLimitExceptionHandler — 429 响应

**Files:**
- Create: `backend/src/main/java/com/hillcommerce/framework/web/RateLimitExceptionHandler.java`
- Create: `backend/src/test/java/com/hillcommerce/framework/ratelimit/RateLimitExceptionHandlerTest.java`

- [ ] **Step 1: 编写 WebMvc 测试**

创建 `backend/src/test/java/com/hillcommerce/framework/ratelimit/RateLimitExceptionHandlerTest.java`：

```java
package com.hillcommerce.framework.ratelimit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hillcommerce.framework.web.ErrorCode;
import com.hillcommerce.framework.web.RateLimitExceptionHandler;

class RateLimitExceptionHandlerTest {

    @RestController
    static class Test12sController {
        @GetMapping("/test-429-12s")
        String trigger12s() {
            throw new RateLimitExceededException("请稍后再试", 12_000_000_000L);
        }
    }

    @RestController
    static class TestSubSecondController {
        @GetMapping("/test-429-subsecond")
        String triggerSubsecond() {
            throw new RateLimitExceededException("稍后", 500_000L);
        }
    }

    @RestController
    static class Test12_1sController {
        @GetMapping("/test-429-12_1s")
        String trigger12_1s() {
            throw new RateLimitExceededException("稍后", 12_100_000_000L);
        }
    }

    private final MockMvc mockMvc12s = MockMvcBuilders
        .standaloneSetup(new Test12sController())
        .setControllerAdvice(new RateLimitExceptionHandler())
        .build();

    private final MockMvc mockMvcSubSecond = MockMvcBuilders
        .standaloneSetup(new TestSubSecondController())
        .setControllerAdvice(new RateLimitExceptionHandler())
        .build();

    private final MockMvc mockMvc12_1s = MockMvcBuilders
        .standaloneSetup(new Test12_1sController())
        .setControllerAdvice(new RateLimitExceptionHandler())
        .build();

    @Test
    void shouldReturn429With12sRetryAfter() throws Exception {
        mockMvc12s.perform(get("/test-429-12s"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value(ErrorCode.RATE_LIMIT_EXCEEDED.code()))
            .andExpect(jsonPath("$.message").value("请稍后再试"))
            .andExpect(jsonPath("$.retryAfter").value(12))
            .andExpect(header().string("Retry-After", "12"));
    }

    @Test
    void shouldCeilToAtLeast1Second() throws Exception {
        mockMvcSubSecond.perform(get("/test-429-subsecond"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.retryAfter").value(1))
            .andExpect(header().string("Retry-After", "1"));
    }

    @Test
    void shouldCeilTo13For12_1Seconds() throws Exception {
        mockMvc12_1s.perform(get("/test-429-12_1s"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.retryAfter").value(13))
            .andExpect(header().string("Retry-After", "13"));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```powershell
cd backend; ./mvnw test -Dtest=RateLimitExceptionHandlerTest -q
```

- [ ] **Step 3: 实现 RateLimitExceptionHandler**

创建 `backend/src/main/java/com/hillcommerce/framework/web/RateLimitExceptionHandler.java`：

```java
package com.hillcommerce.framework.web;

import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.hillcommerce.framework.ratelimit.RateLimitExceededException;

@RestControllerAdvice
public class RateLimitExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(
            RateLimitExceededException ex, HttpServletResponse response) {
        long nanos = ex.getNanosToWait();
        long retrySeconds = Math.max(1, (nanos + 999_999_999L) / 1_000_000_000L);
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

- [ ] **Step 4: 运行测试确认通过**

```powershell
cd backend; ./mvnw test -Dtest=RateLimitExceptionHandlerTest -q
```

预期：Tests run: 3, Failures: 0。

---

### Task 9: 控制器注解 — 业务接口限流声明

**Files:**
- Modify: `backend/src/main/java/com/hillcommerce/modules/user/web/AuthController.java`
- Modify: `backend/src/main/java/com/hillcommerce/modules/product/web/StorefrontProductController.java`
- Modify: `backend/src/main/java/com/hillcommerce/modules/order/web/OrderCheckoutController.java`
- Modify: `backend/src/main/java/com/hillcommerce/modules/product/web/ProductAdminController.java`

- [ ] **Step 1: AuthController — 登录双维度 + 注册**

修改 `backend/src/main/java/com/hillcommerce/modules/user/web/AuthController.java`，新增 import 并在 login 和 register 方法上加注解：

```java
// 新增 import:
import com.hillcommerce.framework.ratelimit.RateLimit;

// login 方法:
@PostMapping("/login")
@RateLimit(key = "login:ip:#{#clientIp}", capacity = 5, refillTokens = 5,
    refillPeriod = 60, message = "登录尝试过于频繁，请1分钟后再试")
@RateLimit(key = "login:account:#{#request.email}", capacity = 5, refillTokens = 5,
    refillPeriod = 60, message = "该账号登录尝试过于频繁，请1分钟后再试")
public AuthUserResponse login(
    @Valid @RequestBody LoginRequest request,
    HttpServletRequest httpServletRequest,
    HttpServletResponse httpServletResponse) { ... }

// register 方法:
@PostMapping("/register")
@RateLimit(key = "register:#{#clientIp}", capacity = 3, refillTokens = 3,
    refillPeriod = 60, message = "注册过于频繁，请稍后再试")
public ResponseEntity<AuthUserResponse> register(
    @Valid @RequestBody RegisterRequest request) { ... }
```

> 隐私说明：`login:account:<email>` 会将明文邮箱写入内存 key 和日志。首版可接受，后续迭代建议对邮箱做 hash（SHA-256 + truncate）或使用 Aspect 内置的 `#principalKey` 变量。

- [ ] **Step 2: StorefrontProductController — 商品列表、详情、搜索**

修改 `backend/src/main/java/com/hillcommerce/modules/product/web/StorefrontProductController.java`，新增 import，在方法上加注解：

```java
// 新增 import:
import com.hillcommerce.framework.ratelimit.RateLimit;

@GetMapping("/products")
@RateLimit(key = "products:#{#clientIp}", capacity = 60, refillTokens = 30, refillPeriod = 30)
public PagedResponse<ProductCardResponse> listHomeProducts(...) { ... }

@GetMapping("/products/{productId}")
@RateLimit(key = "product-detail:#{#clientIp}", capacity = 90, refillTokens = 30, refillPeriod = 10)
public ProductDetailResponse getProductDetail(@PathVariable Long productId) { ... }

@GetMapping("/search")
@RateLimit(key = "search:#{#clientIp}", capacity = 30, refillTokens = 15, refillPeriod = 30)
public PagedResponse<ProductCardResponse> searchProducts(...) { ... }
```

- [ ] **Step 3: OrderCheckoutController — 下单按用户限流**

修改 `backend/src/main/java/com/hillcommerce/modules/order/web/OrderCheckoutController.java`，新增 import，加注解：

```java
// 新增 import:
import com.hillcommerce.framework.ratelimit.RateLimit;

@PostMapping
@ResponseStatus(HttpStatus.CREATED)
@RateLimit(key = "order-create:#{#principalKey}", capacity = 10, refillTokens = 5,
    refillPeriod = 60, message = "下单过于频繁，请稍后再试")
public CreateOrderResponse createOrder(Authentication authentication) { ... }
```

> `#{#principalKey}` 在已登录时自动为 `user:<id>`，未登录时自动为 `ip:<clientIp>`，由 `RateLimitAspect` 注入。

- [ ] **Step 4: ProductAdminController — 管理后台类级限流**

修改 `backend/src/main/java/com/hillcommerce/modules/product/web/ProductAdminController.java`，新增 import，在类上注解：

```java
// 新增 import:
import com.hillcommerce.framework.ratelimit.RateLimit;

@RestController
@RequestMapping("/api/admin/products")
@RateLimit(key = "admin:#{#principalKey}", capacity = 100, refillTokens = 50, refillPeriod = 30)
public class ProductAdminController { ... }
```

> 类级注解为类内所有方法提供默认限流。如果某个方法需要不同的限流策略，在方法上标注独立的 `@RateLimit` 即可覆盖类级。

- [ ] **Step 5: 验证编译**

```powershell
cd backend; ./mvnw compile -q
```

预期：BUILD SUCCESS。参数名 `request` 应能被 `ParameterNameDiscoverer` 正确发现（已在 Task 1 配置 `-parameters`）。

---

### Task 10: Nginx 配置 — Patch 方式修改

**Files:**
- Modify: `ops/nginx/default.conf`

**原则**：只添加限流指令，不重写整个配置文件，避免误删现有 location（WebSocket、上传大小、gzip 等）。

- [ ] **Step 1: 读取现有配置并做 Patch**

> **重要**：执行前先 `cat ops/nginx/default.conf` 确认当前完整内容。以下为针对当前已读版本的 Patch：

1. 在 `server {` 之前（或 `http {` 块内，取决于现有结构）新增：
```nginx
limit_req_zone $binary_remote_addr zone=api:10m rate=30r/s;
```

> 如果 `http` 块在另一个文件（如 `nginx.conf` 主文件）中，则 `limit_req_zone` 需要加到 `http` 块，`default.conf` 的 `server` 无法直接容纳此指令。实现时需确认项目 Nginx 配置结构。

2. 在 `server` 块内新增：
```nginx
limit_req_status 429;
```

3. 在现有 `location /api/ {` 块内新增：
```nginx
limit_req zone=api burst=10 nodelay;
```

**完整 Patch 后 `default.conf` 预期内容**（基于当前已读版本，只新增了 3 行标记为 `# ← NEW`）：

```nginx
limit_req_zone $binary_remote_addr zone=api:10m rate=30r/s;

server {
    listen 80;
    server_name _;
    limit_req_status 429;

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

- [ ] **Step 2: 验证 Nginx 配置语法**

```powershell
docker run --rm -v ${PWD}/ops/nginx/default.conf:/etc/nginx/conf.d/default.conf:ro nginx:alpine nginx -t
```

> 不使用 `nginx -t -c ops/nginx/default.conf`，因为 default.conf 是 conf.d 片段不是完整主配置。

---

### Task 11: AOP 集成测试 — 完整链路验证

**Files:**
- Create: `backend/src/test/java/com/hillcommerce/ratelimit/RateLimitAopIntegrationTest.java`

使用 `@SpringBootTest` + `@AutoConfigureMockMvc` 加载完整 Spring 上下文，让 `RateLimitAspect` 真正生效。

- [ ] **Step 1: 编写 AOP 集成测试**

创建 `backend/src/test/java/com/hillcommerce/ratelimit/RateLimitAopIntegrationTest.java`：

```java
package com.hillcommerce.ratelimit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hillcommerce.framework.ratelimit.ClientIpResolver;
import com.hillcommerce.framework.ratelimit.LocalRateLimitBucketProvider;
import com.hillcommerce.framework.ratelimit.RateLimit;
import com.hillcommerce.framework.ratelimit.RateLimitAspect;
import com.hillcommerce.framework.ratelimit.RateLimitProperties;
import com.hillcommerce.framework.web.RateLimitExceptionHandler;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(
    properties = "hill.rate-limit.enabled=true",
    classes = {
        RateLimitAopIntegrationTest.TestConfig.class,
        RateLimitAopIntegrationTest.TestController.class,
        RateLimitAspect.class,
        LocalRateLimitBucketProvider.class,
        ClientIpResolver.class,
        RateLimitExceptionHandler.class,
        AopAutoConfiguration.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
class RateLimitAopIntegrationTest {

    @Configuration
    static class TestConfig {

        @Bean
        RateLimitProperties rateLimitProperties() {
            return new RateLimitProperties(true);
        }
    }

    @RestController
    static class TestController {

        @GetMapping("/aop-test")
        @RateLimit(key = "aop:#{#clientIp}", capacity = 2, refillTokens = 2,
            refillPeriod = 60, refillUnit = TimeUnit.SECONDS)
        String test() {
            return "ok";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturn200ForFirstTwoRequestsThen429() throws Exception {
        // 显式设置 remoteAddr 使其为非可信代理 IP，确保 clientIp 可控
        var request = get("/aop-test")
            .with(r -> { r.setRemoteAddr("203.0.113.10"); return r; });

        mockMvc.perform(request).andExpect(status().isOk());
        mockMvc.perform(request).andExpect(status().isOk());
        mockMvc.perform(request)
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value(9001));
    }
}
```

- [ ] **Step 2: 运行集成测试**

```powershell
cd backend; ./mvnw test -Dtest=RateLimitAopIntegrationTest -q
```

预期：Tests run: 1, Failures: 0。

---

### Task 12: 全量编译 + 回归验证

- [ ] **Step 1: 运行全部限流相关测试**

```powershell
cd backend; ./mvnw test -Dtest="ClientIpResolverTest,LocalRateLimitBucketProviderTest,RateLimitAspectTest,RateLimitExceptionHandlerTest,RateLimitAopIntegrationTest" -q
```

- [ ] **Step 2: 确认现有测试无回归**

```powershell
cd backend; ./mvnw test -q
```

- [ ] **Step 3: 全量编译**

```powershell
cd backend; ./mvnw compile -q
```

预期：全部 BUILD SUCCESS，所有测试通过。

---

## 文件变更汇总

| 文件完整路径 | 操作 |
|------|------|
| `backend/pom.xml` | 修改（加 bucket4j_jdk17-core 依赖 + `-parameters`） |
| `backend/src/main/resources/application.yml` | 修改（加 `hill.rate-limit.enabled`） |
| `backend/src/main/resources/application-dev.yml` | 修改（加 `hill.rate-limit.enabled: false`） |
| `backend/src/main/java/com/hillcommerce/framework/web/ErrorCode.java` | 修改（加 RATE_LIMIT_EXCEEDED） |
| `backend/src/main/java/com/hillcommerce/framework/ratelimit/RateLimit.java` | 新建 |
| `backend/src/main/java/com/hillcommerce/framework/ratelimit/RateLimitProperties.java` | 新建 |
| `backend/src/main/java/com/hillcommerce/framework/ratelimit/RateLimitExceededException.java` | 新建 |
| `backend/src/main/java/com/hillcommerce/framework/ratelimit/RateLimitBucketProvider.java` | 新建 |
| `backend/src/main/java/com/hillcommerce/framework/ratelimit/LocalRateLimitBucketProvider.java` | 新建 |
| `backend/src/main/java/com/hillcommerce/framework/ratelimit/ClientIpResolver.java` | 新建 |
| `backend/src/main/java/com/hillcommerce/framework/ratelimit/RateLimitAspect.java` | 新建 |
| `backend/src/main/java/com/hillcommerce/framework/web/RateLimitExceptionHandler.java` | 新建 |
| `backend/src/main/java/com/hillcommerce/modules/user/web/AuthController.java` | 修改（登录双维度 + 注册注解） |
| `backend/src/main/java/com/hillcommerce/modules/product/web/StorefrontProductController.java` | 修改（商品列表/详情/搜索注解） |
| `backend/src/main/java/com/hillcommerce/modules/order/web/OrderCheckoutController.java` | 修改（下单注解） |
| `backend/src/main/java/com/hillcommerce/modules/product/web/ProductAdminController.java` | 修改（管理后台类级注解） |
| `ops/nginx/default.conf` | 修改（限流 Patch：3 条指令） |
| `backend/src/test/java/com/hillcommerce/framework/ratelimit/ClientIpResolverTest.java` | 新建 |
| `backend/src/test/java/com/hillcommerce/framework/ratelimit/LocalRateLimitBucketProviderTest.java` | 新建 |
| `backend/src/test/java/com/hillcommerce/framework/ratelimit/RateLimitAspectTest.java` | 新建 |
| `backend/src/test/java/com/hillcommerce/framework/ratelimit/RateLimitExceptionHandlerTest.java` | 新建 |
| `backend/src/test/java/com/hillcommerce/ratelimit/RateLimitAopIntegrationTest.java` | 新建 |

---

## 执行顺序依赖

```
Task 1 (deps, -parameters)
  │
Task 2 (ErrorCode)
  │
Task 3 (ClientIpResolver)     Task 4 (annotation+properties)
  │                             │
  │                           Task 5 (exception)
  │                             │
  └──────────┬──────────────────┘
             │
         Task 6 (bucket provider)
             │
         Task 7 (RateLimitAspect)
             │
         Task 8 (ExceptionHandler)
             │
         Task 9 (controller annotations)
             │
         Task 10 (Nginx) ──────┤
             │                 │
Task 9 (annotate controllers)  │
             │                 │
         Task 11 (AOP integration)
             │
         Task 12 (full verify)
```

## 后续任务

1. **Redis 分布式实现**：新建 `RedisRateLimitBucketProvider`，依赖 `bucket4j_jdk17-redis-common` + `bucket4j_jdk17-redisson`，配置 `RedissonClient` 和 `redis-prefix`，增加 `RATE_LIMIT_UNAVAILABLE(9002, 503)` 错误码和 handler
2. **登录账号维度限流优化**：对邮箱做 SHA-256 hash 减少隐私暴露
3. **本地 bucket 淘汰机制**：如继续本地实现，引入 Caffeine 做 TTL 淘汰
4. **Micrometer 指标**：`rate_limit.rejected.count`、`rate_limit.bucket_error.count`
5. **其他 Admin 控制器注解**：`AdminUserController`、`AdminShopController` 等
6. **`@SkipRateLimit`**：类级注解豁免

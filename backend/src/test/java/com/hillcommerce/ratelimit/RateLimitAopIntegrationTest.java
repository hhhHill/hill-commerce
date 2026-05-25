package com.hillcommerce.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hillcommerce.framework.ratelimit.ClientIpResolver;
import com.hillcommerce.framework.ratelimit.LocalRateLimitBucketProvider;
import com.hillcommerce.framework.ratelimit.RateLimit;
import com.hillcommerce.framework.ratelimit.RateLimitAspect;
import com.hillcommerce.framework.ratelimit.RateLimitExceededException;
import com.hillcommerce.framework.ratelimit.RateLimitProperties;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 集成测试：验证 RateLimitAspect + LocalRateLimitBucketProvider + ClientIpResolver
 * 使用真实 Bucket4j 桶（非 mock provider），验证完整链路。
 */
class RateLimitAopIntegrationTest {

    static class TestTarget {
        @RateLimit(key = "aop-int:#{#clientIp}", capacity = 2, refillTokens = 2,
            refillPeriod = 60, refillUnit = TimeUnit.SECONDS)
        public String limited() {
            return "ok";
        }
    }

    private RateLimitAspect aspect;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties(true);
        ClientIpResolver clientIpResolver = new ClientIpResolver();
        LocalRateLimitBucketProvider bucketProvider = new LocalRateLimitBucketProvider();
        aspect = new RateLimitAspect(properties, clientIpResolver, bucketProvider);
    }

    @Test
    void shouldRejectThirdRequestWithRealBucket() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint("limited", "203.0.113.42");
        setupRequest("203.0.113.42");
        when(joinPoint.proceed()).thenReturn("ok");

        // 前 2 次成功（容量=2）
        aspect.around(joinPoint);
        aspect.around(joinPoint);

        // 第 3 次应被拒绝
        assertThrows(RateLimitExceededException.class, () -> aspect.around(joinPoint));
    }

    @Test
    void shouldSeparateKeys() throws Throwable {
        ProceedingJoinPoint joinPointA = mockJoinPoint("limited", "203.0.113.1");
        ProceedingJoinPoint joinPointB = mockJoinPoint("limited", "203.0.113.2");
        setupRequest("203.0.113.1");
        when(joinPointA.proceed()).thenReturn("ok");
        when(joinPointB.proceed()).thenReturn("ok");

        // IP A 用自己的桶
        aspect.around(joinPointA);
        aspect.around(joinPointA);
        assertThrows(RateLimitExceededException.class, () -> aspect.around(joinPointA));

        // IP B 不应受影响——重新设置请求
        setupRequest("203.0.113.2");
        assertEquals("ok", aspect.around(joinPointB));
    }

    private ProceedingJoinPoint mockJoinPoint(String methodName, String targetClassHint) {
        try {
            java.lang.reflect.Method method = TestTarget.class.getMethod("limited");
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            MethodSignature signature = mock(MethodSignature.class);
            when(signature.getMethod()).thenReturn(method);
            when(joinPoint.getSignature()).thenReturn(signature);
            when(joinPoint.getTarget()).thenReturn(new TestTarget());
            when(joinPoint.getArgs()).thenReturn(new Object[0]);
            return joinPoint;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setupRequest(String remoteAddr) {
        RequestContextHolder.resetRequestAttributes();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(remoteAddr);
        ServletRequestAttributes attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);
    }
}

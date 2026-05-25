package com.hillcommerce.framework.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.bucket4j.ConsumptionProbe;
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

        ConsumptionProbe rejected = ConsumptionProbe.rejected(5_000_000_000L, 0L, 5_000_000_000L);
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

        ConsumptionProbe consumed = ConsumptionProbe.consumed(0L, 0L);
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
            .thenReturn(ConsumptionProbe.consumed(0L, 0L));
        when(bucketProvider.tryConsumeAndReturnRemaining(
            eq("dim-b:203.0.113.5"), any(RateLimit.class)))
            .thenReturn(ConsumptionProbe.rejected(5_000_000_000L, 0L, 5_000_000_000L));

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

package com.hillcommerce.framework.ratelimit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.bucket4j.ConsumptionProbe;
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

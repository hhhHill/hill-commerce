package com.hillcommerce.framework.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
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

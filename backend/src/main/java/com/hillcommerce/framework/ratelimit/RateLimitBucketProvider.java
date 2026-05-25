package com.hillcommerce.framework.ratelimit;

import io.github.bucket4j.ConsumptionProbe;

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

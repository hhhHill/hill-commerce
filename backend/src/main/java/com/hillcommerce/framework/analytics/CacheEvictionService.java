package com.hillcommerce.framework.analytics;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

@Component
public class CacheEvictionService {

    @CacheEvict(value = {"dashboard", "aggregateProfiles"}, allEntries = true)
    public void evictAnalyticsCaches() {
        // 注解驱动
    }
}

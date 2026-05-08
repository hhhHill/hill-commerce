package com.hillcommerce.framework.cache;

import java.time.Duration;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class NoopCacheService implements CacheService {

    @Override
    public Optional<String> get(String key) {
        return Optional.empty();
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        // Redis is not a hard dependency in MVP v1.
    }

    @Override
    public void evict(String key) {
        // Redis is not a hard dependency in MVP v1.
    }
}

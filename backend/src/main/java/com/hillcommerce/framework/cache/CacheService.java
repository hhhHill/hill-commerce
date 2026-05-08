package com.hillcommerce.framework.cache;

import java.time.Duration;
import java.util.Optional;

public interface CacheService {

    Optional<String> get(String key);

    void put(String key, String value, Duration ttl);

    void evict(String key);
}

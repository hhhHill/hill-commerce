package com.hillcommerce.framework.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "hill.rate-limit")
public record RateLimitProperties(
    @DefaultValue("true") boolean enabled
) {}

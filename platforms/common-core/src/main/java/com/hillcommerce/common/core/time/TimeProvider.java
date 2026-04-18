package com.hillcommerce.common.core.time;

import java.time.Instant;

@FunctionalInterface
public interface TimeProvider {

    Instant now();
}

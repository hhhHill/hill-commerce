package com.hillcommerce.common.core.time;

import java.time.Instant;

public interface TimeProvider {

    Instant now();
}

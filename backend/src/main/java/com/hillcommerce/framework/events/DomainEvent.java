package com.hillcommerce.framework.events;

import java.time.Instant;

public interface DomainEvent {

    String eventType();

    Instant occurredAt();
}

package com.hillcommerce.common.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalDomainEventPublisherTest {

    @Test
    void shouldPublishEventToRegisteredHandler() {
        AtomicReference<String> eventType = new AtomicReference<>();
        LocalDomainEventPublisher publisher = new LocalDomainEventPublisher();
        publisher.register("USER_REGISTERED", event -> eventType.set(event.eventType()));

        publisher.publish(new TestDomainEvent("USER_REGISTERED", Instant.now()));

        assertEquals("USER_REGISTERED", eventType.get());
    }

    record TestDomainEvent(String eventType, Instant occurredAt) implements DomainEvent {
    }
}

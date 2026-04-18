package com.hillcommerce.common.event;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class LocalDomainEventPublisher implements DomainEventPublisher {

    private final Map<String, List<Consumer<DomainEvent>>> handlers = new HashMap<>();

    public void register(String eventType, Consumer<DomainEvent> handler) {
        handlers.computeIfAbsent(eventType, key -> new ArrayList<>()).add(handler);
    }

    @Override
    public void publish(DomainEvent event) {
        handlers.getOrDefault(event.eventType(), List.of()).forEach(handler -> handler.accept(event));
    }
}

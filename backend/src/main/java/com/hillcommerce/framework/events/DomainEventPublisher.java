package com.hillcommerce.framework.events;

public interface DomainEventPublisher {

    void publish(DomainEvent event);
}

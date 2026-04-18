package com.hillcommerce.common.event;

public interface DomainEventPublisher {

    void publish(DomainEvent event);
}

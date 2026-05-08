package com.hillcommerce.framework.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class LocalDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LocalDomainEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    public LocalDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(DomainEvent event) {
        log.debug("Publishing domain event locally: {}", event.eventType());
        applicationEventPublisher.publishEvent(event);
    }
}

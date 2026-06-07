package com.sep.vox.infrastructure.event.publisher;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.ExternalEventPublisherPort;

@Component
@ConditionalOnMissingBean(ExternalEventPublisherPort.class)
public class NoOpExternalEventPublisher implements ExternalEventPublisherPort {

    @Override
    public void publish(Object event) {
    }
}

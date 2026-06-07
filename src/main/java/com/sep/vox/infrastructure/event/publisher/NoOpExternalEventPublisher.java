package com.sep.vox.infrastructure.event.publisher;

import com.sep.vox.application.port.output.ExternalEventPublisherPort;

public class NoOpExternalEventPublisher implements ExternalEventPublisherPort {

    @Override
    public void publish(Object event) {
    }
}

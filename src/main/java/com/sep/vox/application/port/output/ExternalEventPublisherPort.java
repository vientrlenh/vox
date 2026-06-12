package com.sep.vox.application.port.output;

public interface ExternalEventPublisherPort {
    void publish(Object event);
}

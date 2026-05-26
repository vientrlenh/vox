package com.sep.vox.application.port.output;

public interface EventPublisherPort {
    void publish(Object event);
}

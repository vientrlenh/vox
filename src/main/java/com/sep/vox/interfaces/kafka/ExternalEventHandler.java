package com.sep.vox.interfaces.kafka;

import tools.jackson.databind.JsonNode;

@FunctionalInterface
public interface ExternalEventHandler {
    void handle(String eventType, JsonNode payload);
}

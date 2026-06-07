package com.sep.vox.application.port.input;

import com.fasterxml.jackson.databind.JsonNode;

@FunctionalInterface
public interface ExternalEventHandler {
    void handle(String eventType, JsonNode payload);
}

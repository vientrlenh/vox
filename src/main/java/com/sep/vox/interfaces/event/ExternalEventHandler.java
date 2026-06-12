package com.sep.vox.interfaces.event;

import com.fasterxml.jackson.databind.JsonNode;

@FunctionalInterface
public interface ExternalEventHandler {
    void handle(String eventType, JsonNode payload);
}

package com.sep.vox.application.port.input.handler.dummy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sep.vox.application.port.input.ExternalEventHandler;

@Component
public class DummyReportGeneratedExternalEventHandler implements ExternalEventHandler {

    private static final Logger log = LoggerFactory.getLogger(DummyReportGeneratedExternalEventHandler.class);
    private static final String EVENT_TYPE = "DummyReportGeneratedExternalEvent";

    @Override
    public void handle(String eventType, JsonNode payload) {
        if (!EVENT_TYPE.equals(eventType)) {
            return;
        }

        log.info(
            "Dummy consume report generated event: reportId={}, requestedBy={}, status={}",
            textOf(payload, "reportId"),
            textOf(payload, "requestedBy"),
            textOf(payload, "status")
        );
    }

    private String textOf(JsonNode payload, String fieldName) {
        var node = payload == null ? null : payload.get(fieldName);
        return node == null || node.isNull() ? null : node.asText();
    }
}

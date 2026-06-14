package com.sep.vox.interfaces.kafka.handler.dummy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


import com.sep.vox.interfaces.kafka.ExternalEventHandler;
import com.sep.vox.interfaces.kafka.mapper.DummyReportGeneratedEventMapper;

import tools.jackson.databind.JsonNode;

@Component
public class DummyReportGeneratedExternalEventHandler implements ExternalEventHandler {

    private static final Logger log = LoggerFactory.getLogger(DummyReportGeneratedExternalEventHandler.class);
    private static final String EVENT_TYPE = "DummyReportGeneratedExternalEvent";

    @Override
    public void handle(String eventType, JsonNode payload) {
        if (!EVENT_TYPE.equals(eventType)) {
            return;
        }

        var data = DummyReportGeneratedEventMapper.fromJson(payload);
        log.info(
            "Dummy consume report generated event: reportId={}, requestedBy={}, status={}",
            data.reportId(), data.requestedBy(), data.status()
        );
    }
}

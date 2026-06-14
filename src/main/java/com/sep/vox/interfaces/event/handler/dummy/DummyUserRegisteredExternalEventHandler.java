package com.sep.vox.interfaces.event.handler.dummy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


import com.sep.vox.interfaces.event.ExternalEventHandler;
import com.sep.vox.interfaces.event.mapper.DummyUserRegisteredEventMapper;

import tools.jackson.databind.JsonNode;

@Component
public class DummyUserRegisteredExternalEventHandler implements ExternalEventHandler {

    private static final Logger log = LoggerFactory.getLogger(DummyUserRegisteredExternalEventHandler.class);
    private static final String EVENT_TYPE = "DummyUserRegisteredExternalEvent";

    @Override
    public void handle(String eventType, JsonNode payload) {
        if (!EVENT_TYPE.equals(eventType)) {
            return;
        }

        var data = DummyUserRegisteredEventMapper.fromJson(payload);
        log.info(
            "Dummy consume user registered event: userId={}, email={}, fullName={}",
            data.userId(), data.email(), data.fullName()
        );
    }
}

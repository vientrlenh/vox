package com.sep.vox.interfaces.kafka.handler.dummy;


import org.springframework.stereotype.Component;


import com.sep.vox.interfaces.kafka.ExternalEventHandler;
import com.sep.vox.interfaces.kafka.mapper.DummyUserRegisteredEventMapper;

import tools.jackson.databind.JsonNode;

@Component
public class DummyUserRegisteredExternalEventHandler implements ExternalEventHandler {

    private static final String EVENT_TYPE = "DummyUserRegisteredExternalEvent";

    @Override
    public void handle(String eventType, JsonNode payload) {
        if (!EVENT_TYPE.equals(eventType)) {
            return;
        }

        var data = DummyUserRegisteredEventMapper.fromJson(payload);
        // userRegisterUseCase.execute(new DummyUserRegisteredCommand(data.userId(), data.email(), data.fullName()))
    }
}

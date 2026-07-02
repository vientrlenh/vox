package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateQuestionTopicStatusCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionTopicStatusRequest;

public final class UpdateQuestionTopicStatusCommandMapper {

    private UpdateQuestionTopicStatusCommandMapper() {
    }

    public static UpdateQuestionTopicStatusCommand fromRequest(UUID id, UpdateQuestionTopicStatusRequest request) {
        return new UpdateQuestionTopicStatusCommand(id, request.action());
    }
}

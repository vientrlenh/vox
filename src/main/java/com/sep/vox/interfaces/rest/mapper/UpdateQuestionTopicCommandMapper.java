package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateQuestionTopicCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionTopicRequest;

public final class UpdateQuestionTopicCommandMapper {

    public static UpdateQuestionTopicCommand fromRequest(UUID id, UpdateQuestionTopicRequest request) {
        return new UpdateQuestionTopicCommand(
            id,
            request.bankId(),
            request.topicName(),
            request.description()
        );
    }
}

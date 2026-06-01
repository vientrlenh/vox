package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateQuestionCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionRequest;

public final class UpdateQuestionCommandMapper {

    public static UpdateQuestionCommand fromRequest(UUID id, UpdateQuestionRequest request) {
        return new UpdateQuestionCommand(
            id,
            request.topicId(),
            request.questionText(),
            request.audioUrl(),
            request.standardLevelId(),
            request.questionType(),
            request.durationSeconds(),
            request.isActive()
        );
    }
}

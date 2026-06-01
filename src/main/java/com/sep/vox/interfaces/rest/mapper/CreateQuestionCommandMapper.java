package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateQuestionCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateQuestionRequest;

public final class CreateQuestionCommandMapper {

    public static CreateQuestionCommand fromRequest(CreateQuestionRequest request) {
        return new CreateQuestionCommand(
            request.topicId(),
            request.questionText(),
            request.audioUrl(),
            request.standardLevelId(),
            request.questionType(),
            request.durationSeconds()
        );
    }
}

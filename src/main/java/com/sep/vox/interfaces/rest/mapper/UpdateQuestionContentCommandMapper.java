package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateQuestionContentCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionContentRequest;

public final class UpdateQuestionContentCommandMapper {

    private UpdateQuestionContentCommandMapper() {
    }

    public static UpdateQuestionContentCommand fromRequest(UUID questionId, UpdateQuestionContentRequest request) {
        return new UpdateQuestionContentCommand(
            questionId,
            request.instructionText(),
            request.questionText(),
            request.promptText(),
            request.preparationText(),
            request.type(),
            request.preparationTimeSeconds(),
            request.minResponseSeconds(),
            request.maxResponseSeconds()
        );
    }
}

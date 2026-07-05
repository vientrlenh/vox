package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateQuestionCollaboratorCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateQuestionCollaboratorRequest;

public final class CreateQuestionCollaboratorCommandMapper {

    private CreateQuestionCollaboratorCommandMapper() {
    }

    public static CreateQuestionCollaboratorCommand fromRequest(UUID questionId, CreateQuestionCollaboratorRequest request) {
        return new CreateQuestionCollaboratorCommand(
            questionId,
            request.userId(),
            request.permission()
        );
    }
}

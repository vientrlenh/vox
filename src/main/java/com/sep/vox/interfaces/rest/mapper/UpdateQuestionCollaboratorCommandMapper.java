package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateQuestionCollaboratorCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionCollaboratorRequest;

public final class UpdateQuestionCollaboratorCommandMapper {

    private UpdateQuestionCollaboratorCommandMapper() {
    }

    public static UpdateQuestionCollaboratorCommand fromRequest(
            UUID questionId,
            UUID collaboratorId,
            UpdateQuestionCollaboratorRequest request) {
        return new UpdateQuestionCollaboratorCommand(
            questionId,
            collaboratorId,
            request.permission()
        );
    }
}

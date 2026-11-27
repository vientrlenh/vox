package com.sep.vox.interfaces.rest.dto.request.question;

import com.sep.vox.application.port.input.command.question.UpdateQuestionCollaboratorCommand;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record UpdateQuestionCollaboratorRequest(
    @NotNull(message = "Collaboration permission must not be blank")
    QuestionCollaboratorPermission permission
) {

    public static UpdateQuestionCollaboratorCommand toCommand(
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

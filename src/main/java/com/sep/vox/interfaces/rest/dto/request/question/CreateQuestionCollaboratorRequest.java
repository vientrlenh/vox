package com.sep.vox.interfaces.rest.dto.request.question;

import java.util.UUID;

import com.sep.vox.application.port.input.command.question.CreateQuestionCollaboratorCommand;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;

import jakarta.validation.constraints.NotNull;

public record CreateQuestionCollaboratorRequest(
    @NotNull(message = "ID người cộng tác không được để trống")
    UUID userId,

    @NotNull(message = "Quyền cộng tác không được để trống")
    QuestionCollaboratorPermission permission
) {

    public static CreateQuestionCollaboratorCommand toCommand(UUID questionId, CreateQuestionCollaboratorRequest request) {
        return new CreateQuestionCollaboratorCommand(
            questionId,
            request.userId(),
            request.permission()
        );
    }
}

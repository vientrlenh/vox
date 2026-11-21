package com.sep.vox.interfaces.rest.dto.request;

import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;

import jakarta.validation.constraints.NotNull;

public record UpdateQuestionCollaboratorRequest(
    @NotNull(message = "Quyền cộng tác không được để trống")
    QuestionCollaboratorPermission permission
) {
}

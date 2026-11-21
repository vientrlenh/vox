package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;

import jakarta.validation.constraints.NotNull;

public record CreateQuestionCollaboratorRequest(
    @NotNull(message = "ID người cộng tác không được để trống")
    UUID userId,

    @NotNull(message = "Quyền cộng tác không được để trống")
    QuestionCollaboratorPermission permission
) {
}

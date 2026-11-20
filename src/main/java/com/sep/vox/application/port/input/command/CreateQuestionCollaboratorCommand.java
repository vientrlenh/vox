package com.sep.vox.application.port.input.command;

import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;

public record CreateQuestionCollaboratorCommand(
    UUID questionId,
    UUID userId,
    QuestionCollaboratorPermission permission
) {
}

package com.sep.vox.application.port.input.command;

import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;

public record UpdateQuestionCollaboratorCommand(
    UUID questionId,
    UUID collaboratorId,
    QuestionCollaboratorPermission permission
) {
}

package com.sep.vox.application.port.input.command.question;

import java.util.UUID;

public record DeleteQuestionCollaboratorCommand(
    UUID questionId,
    UUID collaboratorId
) {
}

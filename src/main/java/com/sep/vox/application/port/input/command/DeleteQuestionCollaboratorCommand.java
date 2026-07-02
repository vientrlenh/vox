package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record DeleteQuestionCollaboratorCommand(
    UUID questionId,
    UUID collaboratorId
) {
}

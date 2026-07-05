package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateQuestionCollaboratorCommand(
    UUID questionId,
    UUID collaboratorId,
    String permission
) {
}

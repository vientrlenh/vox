package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CreateQuestionCollaboratorCommand(
    UUID questionId,
    UUID userId,
    String permission
) {
}

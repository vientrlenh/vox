package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateQuestionCommand(
    UUID id,
    String instructionText,
    String questionText,
    String promptText,
    String preparationText,
    String questionType,
    Integer preparationTimeSeconds,
    Integer minResponseSeconds,
    Integer maxResponseSeconds,
    String sharing
) {
}

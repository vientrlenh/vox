package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateQuestionContentCommand(
    UUID questionId,
    String instructionText,
    String questionText,
    String promptText,
    String preparationText,
    String type,
    String scope,
    String visibility,
    int preparationTimeSeconds,
    int minResponseSeconds,
    int maxResponseSeconds
) {
}

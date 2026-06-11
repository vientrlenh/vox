package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CreateSchoolQuestionBankQuestionCommand(
    UUID questionTopicId,
    String code,
    String instructionText,
    String questionText,
    String promptText,
    String preparationText,
    String type,
    int preparationTimeSeconds,
    int minResponseSeconds,
    int maxResponseSeconds
) {
}

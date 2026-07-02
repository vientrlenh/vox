package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record CreateSystemQuestionBankQuestionCommand(
    UUID questionBankId,
    UUID questionTopicId,
    String code,
    String instructionText, 
    String questionText,
    String promptText, 
    String preparationText,
    String type,
    int preparationTimeSeconds,
    int minResponseSeconds, 
    int maxResponseSeconds,
    String sharing,
    List<CreateQuestionAssetCommand> assets,
    CreateQuestionEvaluationGuideCommand evaluationGuide
) {
}

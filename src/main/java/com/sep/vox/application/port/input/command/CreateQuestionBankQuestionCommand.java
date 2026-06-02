package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record CreateQuestionBankQuestionCommand(
    UUID questionTopicId,
    String code,
    String instructionText, 
    String questionText,
    String promptText, 
    String preparationText,
    UUID standardLevelVersionId,
    UUID schoolLevelVersionId,
    String expectedContent,
    String keyPoints, 
    String acceptableResponses, 
    String offTopicExamples,
    String scoringHints, 
    String commonMistakes,
    String type,
    int preparationTimeSeconds,
    int minResponseSeconds, 
    int maxResponseSeconds,
    String status,
    List<CreateQuestionAssetCommand> assets
) {
}

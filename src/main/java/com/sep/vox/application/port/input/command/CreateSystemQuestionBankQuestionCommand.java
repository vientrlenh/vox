package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionType;

public record CreateSystemQuestionBankQuestionCommand(
    UUID questionBankId,
    UUID questionTopicId,
    String code,
    String instructionText, 
    String questionText,
    String promptText, 
    String preparationText,
    QuestionType type,
    int preparationTimeSeconds,
    int minResponseSeconds, 
    int maxResponseSeconds,
    QuestionSharing sharing,
    List<CreateQuestionAssetCommand> assets,
    CreateQuestionEvaluationGuideCommand evaluationGuide
) {
}

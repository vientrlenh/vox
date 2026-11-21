package com.sep.vox.application.port.input.command;

import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionType;

public record UpdateQuestionCommand(
    UUID id,
    String instructionText,
    String questionText,
    String promptText,
    String preparationText,
    QuestionType questionType,
    Integer preparationTimeSeconds,
    Integer minResponseSeconds,
    Integer maxResponseSeconds,
    QuestionSharing sharing
) {
}

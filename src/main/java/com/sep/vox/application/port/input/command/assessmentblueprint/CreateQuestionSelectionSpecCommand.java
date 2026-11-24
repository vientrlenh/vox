package com.sep.vox.application.port.input.command.assessmentblueprint;

import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionDifficulty;
import com.sep.vox.domain.model.question.QuestionType;

public record CreateQuestionSelectionSpecCommand(
    QuestionType questionType,
    QuestionDifficulty difficulty,
    String targetBandLevel,
    String skillCode,
    UUID topicId
) {
}

package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionDifficulty;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;

public record ViewQuestionsQuery(
    UUID questionBankId,
    String topicName,
    QuestionStatus status,
    QuestionType type,
    QuestionSharing sharing,
    QuestionDifficulty difficulty,
    String scope,
    String keyword,
    int page,
    int size
) {
}

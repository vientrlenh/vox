package com.sep.vox.application.port.input.query.question;

import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;

public record ViewQuestionsQuery(
    UUID questionBankId,
    UUID questionTopicId,
    String topicName,
    QuestionStatus status,
    QuestionType type,
    QuestionSharing sharing,
    /** The name of a QuestionAssetType enum, or "NONE" to get questions WITHOUT a resource. null = no filter. */
    String assetType,
    String scope,
    String keyword,
    int page,
    int size
) {
}

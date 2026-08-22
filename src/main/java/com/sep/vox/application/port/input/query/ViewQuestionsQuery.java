package com.sep.vox.application.port.input.query;

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
    /** Tên enum QuestionAssetType, hoặc "NONE" để lấy câu KHÔNG có tài nguyên. null = không lọc. */
    String assetType,
    String scope,
    String keyword,
    int page,
    int size
) {
}

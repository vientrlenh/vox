package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.application.shared.AppPageRequest;
import com.sep.vox.domain.model.question.QuestionTopicStatus;

public record ViewQuestionTopicsQuery(
    UUID questionBankId,
    QuestionTopicStatus status,
    String keyword,
    AppPageRequest pageRequest
) {
}

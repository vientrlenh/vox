package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;

public record ViewQuestionBanksQuery(
    QuestionBankOwnerType ownerType,
    QuestionBankStatus status,
    UUID languageId,
    UUID schoolId,
    UUID schoolGradeId,
    String keyword,
    int page,
    int size
) {
}

package com.sep.vox.application.response.input.question;

import java.util.List;

import com.sep.vox.domain.dto.QuestionBankDto;

public record BulkUpdateQuestionBankStatusResponse(
    List<QuestionBankDto> updated,
    List<BulkUpdateQuestionScopeStatusFailure> failed
) {
}

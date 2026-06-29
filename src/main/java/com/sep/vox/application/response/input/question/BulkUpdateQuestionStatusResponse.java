package com.sep.vox.application.response.input.question;

import java.util.List;

import com.sep.vox.domain.dto.QuestionDto;

public record BulkUpdateQuestionStatusResponse(
    List<QuestionDto> updated,
    List<BulkUpdateQuestionStatusFailure> failed
) {
}

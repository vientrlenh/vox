package com.sep.vox.application.response.input.question;

import java.util.List;

import com.sep.vox.domain.dto.QuestionTopicDto;

public record BulkUpdateQuestionTopicStatusResponse(
    List<QuestionTopicDto> updated,
    List<BulkUpdateQuestionScopeStatusFailure> failed
) {
}

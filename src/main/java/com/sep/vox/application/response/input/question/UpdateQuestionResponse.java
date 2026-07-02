package com.sep.vox.application.response.input.question;

import com.sep.vox.domain.dto.QuestionDto;

public record UpdateQuestionResponse(
    QuestionDto question,
    boolean clonedAsNew
) {
}

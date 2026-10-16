package com.sep.vox.application.response.input.question;

import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.model.question.Question;

public record UpdateQuestionResponse(
    QuestionDto question
) {

    public static UpdateQuestionResponse toResponse(Question question) {
        return new UpdateQuestionResponse(QuestionDto.toDto(question));
    }
}

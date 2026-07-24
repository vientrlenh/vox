package com.sep.vox.application.mapper.question;

import com.sep.vox.application.response.input.question.UpdateQuestionResponse;
import com.sep.vox.domain.mapper.QuestionDtoMapper;
import com.sep.vox.domain.model.question.Question;

public final class UpdateQuestionResponseMapper {

    private UpdateQuestionResponseMapper() {
    }

    public static UpdateQuestionResponse toResponse(Question question) {
        return new UpdateQuestionResponse(
            QuestionDtoMapper.toQuestionDto(question)
        );
    }
}

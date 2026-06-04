package com.sep.vox.application.mapper.question;

import java.util.UUID;

import com.sep.vox.application.response.input.question.CreateQuestionResponse;

public final class CreateQuestionResponseMapper {
    
    public static CreateQuestionResponse toResponse(UUID questionId) {
        return new CreateQuestionResponse(questionId);
    }
}

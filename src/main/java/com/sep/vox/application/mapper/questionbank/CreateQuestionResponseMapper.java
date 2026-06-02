package com.sep.vox.application.mapper.questionbank;

import java.util.UUID;

import com.sep.vox.application.response.input.question.CreateQuestionResponse;

public final class CreateQuestionResponseMapper {
    
    public static CreateQuestionResponse toResponse(UUID questionId) {
        return new CreateQuestionResponse(questionId);
    }
}

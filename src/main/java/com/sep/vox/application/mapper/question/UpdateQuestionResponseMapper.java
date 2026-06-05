package com.sep.vox.application.mapper.question;

import java.util.UUID;

import com.sep.vox.application.response.input.question.UpdateQuestionResponse;

public final class UpdateQuestionResponseMapper {
    
    public static UpdateQuestionResponse toResponse(UUID questionId) {
        return new UpdateQuestionResponse(questionId);
    }
}

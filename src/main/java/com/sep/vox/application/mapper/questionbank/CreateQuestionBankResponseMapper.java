package com.sep.vox.application.mapper.questionbank;

import java.util.UUID;

import com.sep.vox.application.response.input.question.CreateQuestionBankResponse;

public final class CreateQuestionBankResponseMapper {
    
    public static CreateQuestionBankResponse toResponse(UUID questionBankId) {
        return new CreateQuestionBankResponse(questionBankId);
    }
}

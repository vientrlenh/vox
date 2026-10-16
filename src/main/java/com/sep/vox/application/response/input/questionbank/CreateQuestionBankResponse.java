package com.sep.vox.application.response.input.questionbank;

import java.util.UUID;

public record CreateQuestionBankResponse(
    UUID questionBankId
) {

    public static CreateQuestionBankResponse toResponse(UUID questionBankId) {
        return new CreateQuestionBankResponse(questionBankId);
    }
}

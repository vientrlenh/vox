package com.sep.vox.application.response.input.questionbank;

public record DeleteQuestionBankResponse(
    boolean deleted,
    boolean archivedInstead
) {
}

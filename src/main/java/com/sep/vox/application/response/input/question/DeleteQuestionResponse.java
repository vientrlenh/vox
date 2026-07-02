package com.sep.vox.application.response.input.question;

public record DeleteQuestionResponse(
    boolean deleted,
    boolean archivedInstead
) {
}

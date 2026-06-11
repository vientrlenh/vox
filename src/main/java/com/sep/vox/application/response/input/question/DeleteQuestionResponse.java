package com.sep.vox.application.response.input.question;

import java.util.UUID;

public record DeleteQuestionResponse(
    UUID questionId,
    String deleteMode,
    String resultingStatus
) {
}

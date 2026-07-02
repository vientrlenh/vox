package com.sep.vox.application.response.input.question;

import java.util.UUID;

public record BulkUpdateQuestionStatusFailure(
    UUID questionId,
    String reason
) {
}

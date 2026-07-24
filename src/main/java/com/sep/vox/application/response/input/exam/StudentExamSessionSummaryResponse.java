package com.sep.vox.application.response.input.exam;

import java.util.UUID;

public record StudentExamSessionSummaryResponse(
    UUID sessionId,
    int attemptNumber,
    String status,
    boolean flagged
) {
}

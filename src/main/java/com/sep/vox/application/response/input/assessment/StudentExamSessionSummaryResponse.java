package com.sep.vox.application.response.input.assessment;

import java.util.UUID;

public record StudentExamSessionSummaryResponse(
    UUID sessionId,
    int attemptNumber,
    String status,
    boolean flagged
) {
}

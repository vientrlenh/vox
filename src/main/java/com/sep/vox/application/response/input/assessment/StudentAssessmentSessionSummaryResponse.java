package com.sep.vox.application.response.input.assessment;

import java.util.UUID;

public record StudentAssessmentSessionSummaryResponse(
    UUID sessionId,
    int attemptNumber,
    String status,
    boolean flagged
) {
}

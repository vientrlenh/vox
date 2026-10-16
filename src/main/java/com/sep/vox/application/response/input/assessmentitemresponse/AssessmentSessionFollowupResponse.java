package com.sep.vox.application.response.input.assessmentitemresponse;

import java.util.UUID;

public record AssessmentSessionFollowupResponse(
    UUID assessmentItemAnswerId,
    long followupCount,
    long totalTurns
) {
}

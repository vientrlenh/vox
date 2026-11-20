package com.sep.vox.application.response.input.assessmentitemresponse;

import java.util.List;
import java.util.UUID;

public record AssessmentItemResponseDetailsResponse(
    UUID id,
    UUID sessionId,
    UUID paperItemId,
    String audioUrl,
    Integer durationSeconds,
    String transcript,
    String submittedAt,
    List<AssessmentItemResponseTurnResponse> turns
) {
}

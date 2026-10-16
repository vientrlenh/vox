package com.sep.vox.application.response.input.assessmentitemresponse;

import java.util.List;
import java.util.UUID;

public record AssessmentItemAnswerDetailsResponse(
    UUID id,
    UUID sessionId,
    UUID paperItemId,
    String audioUrl,
    Integer durationSeconds,
    String transcript,
    String submittedAt,
    List<AssessmentItemAnswerTurnResponse> turns
) {
}

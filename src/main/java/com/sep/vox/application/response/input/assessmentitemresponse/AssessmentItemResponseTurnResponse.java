package com.sep.vox.application.response.input.assessmentitemresponse;

import com.sep.vox.domain.model.assessment.TurnType;
import java.time.Instant;
import java.util.UUID;

public record AssessmentItemResponseTurnResponse(
    UUID id,
    UUID assessmentItemResponseId,
    int turnOrder,
    TurnType turnType,
    String promptText,
    String audioUrl,
    String transcript,
    Integer durationSeconds,
    Integer wordCount,
    Instant answeredAt,
    Instant createdAt
) {
}

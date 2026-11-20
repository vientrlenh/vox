package com.sep.vox.application.response.input.assessmentitemresponse;

import java.util.UUID;

public record AssessmentItemResponseTurnResponse(
    UUID id,
    UUID assessmentItemResponseId,
    int turnOrder,
    String turnType,
    String promptText,
    String audioUrl,
    String transcript,
    Integer durationSeconds,
    Integer wordCount,
    String answeredAt,
    String createdAt
) {
}

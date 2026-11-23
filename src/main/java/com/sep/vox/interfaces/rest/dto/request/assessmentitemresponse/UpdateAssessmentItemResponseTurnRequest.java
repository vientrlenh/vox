package com.sep.vox.interfaces.rest.dto.request.assessmentitemresponse;

import java.time.Instant;

import com.sep.vox.domain.model.assessment.TurnType;

public record UpdateAssessmentItemResponseTurnRequest(
    TurnType turnType,
    String promptText,
    String audioUrl,
    String transcript,
    Integer durationSeconds,
    Integer wordCount,
    Instant answeredAt
) {
}

package com.sep.vox.application.response.input.assessmentitemresponse;

import java.util.UUID;

public record AssessmentItemEvaluationTurnResponse(
    UUID id,
    Integer turnOrder,
    String turnType,
    String promptText,
    String audioUrl,
    String transcript,
    Integer wordCount,
    Integer durationSeconds,
    Double asrConfidence,
    String pronunciationOverall,
    String wordFeedback
) {
}

package com.sep.vox.application.response.input.assessmentitemresponse;

import com.sep.vox.domain.model.assessment.TurnType;
import java.util.UUID;

public record AssessmentItemEvaluationTurnResponse(
    UUID id,
    Integer turnOrder,
    TurnType turnType,
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

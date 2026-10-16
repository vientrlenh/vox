package com.sep.vox.application.response.input.assessmentitemresponse;

import java.math.BigDecimal;
import java.util.UUID;

public record ExamItemAiEvaluationContextResponse(
    UUID evaluationId,
    String engineType,
    String gradedByModel,
    String promptVersion,
    BigDecimal overallConfidence,
    boolean requiresHumanReview,
    String reviewReasonCode,
    boolean markedInvalid,
    boolean requiresRetake,
    String evaluatedAt,
    String feedbackSummary,
    String signals,
    String validity,
    String suggestions
) {
}

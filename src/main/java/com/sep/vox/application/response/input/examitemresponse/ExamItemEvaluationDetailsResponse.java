package com.sep.vox.application.response.input.examitemresponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ExamItemEvaluationDetailsResponse(
    UUID id,
    UUID responseId,
    UUID paperItemId,
    String engineType,
    String gradedByModel,
    String promptVersion,
    BigDecimal rawItemScore,
    BigDecimal itemScore,
    BigDecimal overallConfidence,
    boolean requiresHumanReview,
    String reviewReasonCode,
    boolean markedInvalid,
    boolean requiresRetake,
    String status,
    String evaluatedAt,
    String feedbackSummary,
    String signals,
    String validity,
    String suggestions,
    List<ExamItemCriterionScoreResponse> criteria,
    List<ExamItemEvaluationTurnResponse> turns
) {
}

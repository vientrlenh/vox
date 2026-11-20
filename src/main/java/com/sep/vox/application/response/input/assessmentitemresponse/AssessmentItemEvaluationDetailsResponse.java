package com.sep.vox.application.response.input.assessmentitemresponse;

import com.sep.vox.domain.model.assessment.AssessmentEvaluationEngineType;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluationStatus;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AssessmentItemEvaluationDetailsResponse(
    UUID id,
    UUID responseId,
    UUID paperItemId,
    AssessmentEvaluationEngineType engineType,
    String gradedByModel,
    String promptVersion,
    BigDecimal rawItemScore,
    BigDecimal itemScore,
    BigDecimal overallConfidence,
    boolean requiresHumanReview,
    String reviewReasonCode,
    boolean markedInvalid,
    boolean requiresRetake,
    AssessmentItemEvaluationStatus status,
    Instant evaluatedAt,
    String feedbackSummary,
    String signals,
    String validity,
    String suggestions,
    List<AssessmentItemCriterionScoreResponse> criteria,
    /**
     * Always taken from the AI version -- only the AI version produces speaking turns. A manual grading has no turns,
     * so reading them from the version in force would make the audio/transcript/question text disappear
     * as soon as a teacher re-grades.
     */
    List<AssessmentItemEvaluationTurnResponse> turns,
    /** AI context; {@code null} when the answer never had an AI version. */
    AssessmentItemAiEvaluationContextResponse ai
) {
}

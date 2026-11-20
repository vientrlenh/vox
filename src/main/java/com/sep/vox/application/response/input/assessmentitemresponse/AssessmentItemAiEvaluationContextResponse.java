package com.sep.vox.application.response.input.assessmentitemresponse;

import com.sep.vox.domain.model.assessment.AssessmentEvaluationEngineType;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Evidence of the AI grading, kept even after a teacher has re-graded (the AI version then carries
 * SUPERSEDED). The displayed score and rationale still belong to the version in force one level up --
 * this block is only CONTEXT.
 *
 * <p>Kept separate instead of back-filling the fields of the manual grading: an
 * {@code overallConfidence} attached to a score given by a person is meaningless, and once mixed the
 * reader of the payload has no way to tell which version a number came from. Same convention as
 * {@code GradingTaskItemInfo} on the teacher's grading screen.
 */
public record AssessmentItemAiEvaluationContextResponse(
    UUID evaluationId,
    AssessmentEvaluationEngineType engineType,
    String gradedByModel,
    String promptVersion,
    BigDecimal overallConfidence,
    boolean requiresHumanReview,
    String reviewReasonCode,
    boolean markedInvalid,
    boolean requiresRetake,
    Instant evaluatedAt,
    String feedbackSummary,
    String signals,
    String validity,
    String suggestions
) {
}

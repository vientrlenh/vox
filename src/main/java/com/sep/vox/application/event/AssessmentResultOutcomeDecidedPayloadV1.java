package com.sep.vox.application.event;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentKind;

/**
 * The result is finalized as {@code PASSED}/{@code FAILED} -- the last milestone in the score
 * lifecycle of an attempt.
 *
 * @param sessionId the student's result screen takes sessionId, not candidateResultId
 * @param assessmentKind centralized assessments and class tests have two separate result screens
 */
public record AssessmentResultOutcomeDecidedPayloadV1(
    UUID candidateResultId,
    UUID studentId,
    String assessmentName,
    String outcome,
    BigDecimal totalScore,
    UUID sessionId,
    AssessmentKind assessmentKind
) {

}

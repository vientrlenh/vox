package com.sep.vox.application.event;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentKind;

/**
 * A score that was ALREADY PUBLISHED has now changed (regraded after a post-check or an appeal).
 *
 * <p>Only published when the score really changes: a post-check that keeps the score must not bother the
 * student with an email saying "your score has just changed" when it has not changed.
 *
 * @param roundType the round that caused the change -- the email says clearly whether it came from a post-check or an appeal
 *
 * @param sessionId the student's result screen takes sessionId, not candidateResultId
 * @param assessmentKind centralized assessments and class tests have two separate result screens
 */
public record AssessmentResultRegradedPayloadV1(
    UUID candidateResultId,
    UUID studentId,
    String assessmentName,
    String roundType,
    BigDecimal scoreBefore,
    BigDecimal scoreAfter,
    UUID sessionId,
    AssessmentKind assessmentKind
) {

}

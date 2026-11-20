package com.sep.vox.application.event;

import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentKind;

/**
 * An attempt whose invalidation was lifted after a second look -- it will be graded again from scratch.
 *
 * @param sessionId the student's result screen takes sessionId, not candidateResultId
 * @param assessmentKind centralized assessments and class tests have two separate result screens
 */
public record AssessmentResultInvalidClearedPayloadV1(
    UUID candidateResultId,
    UUID studentId,
    String assessmentName,
    String reason,
    UUID sessionId,
    AssessmentKind assessmentKind
) {

}

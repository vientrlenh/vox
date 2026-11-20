package com.sep.vox.application.event;

import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentKind;

/**
 * An attempt concluded as a violation -> invalidated. The reason is mandatory, so there is always content to send.
 *
 * @param sessionId the student's result screen takes sessionId, not candidateResultId
 * @param assessmentKind centralized assessments and class tests have two separate result screens
 */
public record AssessmentResultInvalidatedPayloadV1(
    UUID candidateResultId,
    UUID studentId,
    String assessmentName,
    String reason,
    UUID sessionId,
    AssessmentKind assessmentKind
) {

}

package com.sep.vox.application.event.assessment;

import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentKind;

public record AssessmentResultInvalidClearedPayload(
    UUID candidateResultId,
    UUID studentId,
    String assessmentName,
    String reason,
    UUID sessionId,
    AssessmentKind assessmentKind
) {

}

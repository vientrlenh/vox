package com.sep.vox.application.event.assessment;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentKind;

public record AssessmentResultReleasedPayload(
    UUID candidateResultId,
    UUID studentId,
    String assessmentName,
    BigDecimal totalScore,
    UUID sessionId,
    AssessmentKind assessmentKind
) {

}

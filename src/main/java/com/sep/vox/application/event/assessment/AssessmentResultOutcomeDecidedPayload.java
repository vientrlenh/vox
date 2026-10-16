package com.sep.vox.application.event.assessment;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentKind;

public record AssessmentResultOutcomeDecidedPayload(
    UUID candidateResultId,
    UUID studentId,
    String assessmentName,
    String outcome,
    BigDecimal totalScore,
    UUID sessionId,
    AssessmentKind assessmentKind
) {

}

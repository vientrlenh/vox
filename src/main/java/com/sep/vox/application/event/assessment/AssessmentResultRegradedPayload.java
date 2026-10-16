package com.sep.vox.application.event.assessment;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentKind;

public record AssessmentResultRegradedPayload(
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

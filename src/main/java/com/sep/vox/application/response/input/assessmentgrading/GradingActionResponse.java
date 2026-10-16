package com.sep.vox.application.response.input.assessmentgrading;

import java.math.BigDecimal;
import java.util.UUID;

public record GradingActionResponse(
    UUID assignmentId,
    UUID candidateResultId,
    String outcome,
    String resultStatus,
    BigDecimal totalScore,
    UUID nextAssignmentId
) {
}

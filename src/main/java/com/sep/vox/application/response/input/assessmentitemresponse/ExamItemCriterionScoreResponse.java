package com.sep.vox.application.response.input.assessmentitemresponse;

import java.math.BigDecimal;
import java.util.UUID;

public record ExamItemCriterionScoreResponse(
    UUID id,
    UUID rubricCriterionId,
    String criterionCode,
    String criterionName,
    BigDecimal minScore,
    BigDecimal maxScore,
    BigDecimal rawScore,
    BigDecimal finalScore,
    String rationale
) {
}

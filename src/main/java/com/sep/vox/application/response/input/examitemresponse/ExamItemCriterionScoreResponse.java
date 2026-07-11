package com.sep.vox.application.response.input.examitemresponse;

import java.math.BigDecimal;
import java.util.UUID;

public record ExamItemCriterionScoreResponse(
    UUID id,
    UUID rubricCriterionId,
    String criterionCode,
    String criterionName,
    BigDecimal rawScore,
    BigDecimal finalScore,
    String rationale
) {
}

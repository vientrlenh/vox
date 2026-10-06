package com.sep.vox.application.projection.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record GradingCriterionScoreInfo(
    UUID criterionId,
    String criterionCode,
    String label,
    BigDecimal score,
    String rationale
) {
}

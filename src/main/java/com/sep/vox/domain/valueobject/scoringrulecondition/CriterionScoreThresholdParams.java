package com.sep.vox.domain.valueobject.scoringrulecondition;

import java.math.BigDecimal;

public record CriterionScoreThresholdParams(
    String criterionCode,
    BigDecimal score
) implements ScoringRuleConditionParams {
    public CriterionScoreThresholdParams {
        if (criterionCode == null || criterionCode.isBlank()) {
            throw new IllegalArgumentException("Criterion code không được để trống");
        }
        if (score == null || score.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Điểm số không được để trống hoặc dưới 0");
        }
    }
}

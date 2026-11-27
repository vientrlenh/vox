package com.sep.vox.domain.valueobject.scoringrulecondition;

import java.math.BigDecimal;

public record CriterionScoreThresholdParams(
    String criterionCode,
    BigDecimal score
) implements ScoringRuleConditionParams {
    public CriterionScoreThresholdParams {
        if (criterionCode == null || criterionCode.isBlank()) {
            throw new IllegalArgumentException("The criterion code must not be blank");
        }
        if (score == null || score.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("The score must not be blank or below 0");
        }
    }
}

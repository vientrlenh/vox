package com.sep.vox.domain.valueobject.scoringruleaction;

import java.math.BigDecimal;

public record CapCriterionScoreParams(
    String criterionCode,
    BigDecimal maxScore
) implements ScoringRuleActionParams {
    public CapCriterionScoreParams {
        if (criterionCode == null || criterionCode.isBlank()) {
            throw new IllegalArgumentException("The criterion code must not be blank");
        }
        if (maxScore == null || maxScore.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("The maximum score must not be blank or below 0");
        }
    }
}

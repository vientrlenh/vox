package com.sep.vox.domain.valueobject.scoringruleaction;

import java.math.BigDecimal;

public record CriterionScoreDeltaParams(
    String criterionCode,
    BigDecimal delta
) implements ScoringRuleActionParams {
    public CriterionScoreDeltaParams {
        if (criterionCode == null || criterionCode.isBlank()) {
            throw new IllegalArgumentException("The criterion code must not be blank");
        }
        if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("The delta must not be blank or 0");
        }
    }
}

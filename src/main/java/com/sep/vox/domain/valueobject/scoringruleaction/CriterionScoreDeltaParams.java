package com.sep.vox.domain.valueobject.scoringruleaction;

import java.math.BigDecimal;

public record CriterionScoreDeltaParams(
    String criterionCode,
    BigDecimal delta
) implements ScoringRuleActionParams {
    public CriterionScoreDeltaParams {
        if (criterionCode == null || criterionCode.isBlank()) {
            throw new IllegalArgumentException("Code của tiêu chí không được để trống");
        }
        if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Delta không được để trống hoặc bằng 0");
        }
    }
}

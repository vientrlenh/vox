package com.sep.vox.domain.valueobject.scoringruleaction;

import java.math.BigDecimal;

public record CapCriterionScoreParams(
    String criterionCode,
    BigDecimal maxScore
) implements ScoringRuleActionParams {
    public CapCriterionScoreParams {
        if (criterionCode == null || criterionCode.isBlank()) {
            throw new IllegalArgumentException("Code của tiêu chí không được để trống");
        }
        if (maxScore == null || maxScore.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Điểm tối đa không được để trống hoặc dưới 0");
        }
    }
}

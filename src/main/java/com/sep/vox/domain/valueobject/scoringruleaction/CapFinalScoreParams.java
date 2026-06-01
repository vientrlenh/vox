package com.sep.vox.domain.valueobject.scoringruleaction;

import java.math.BigDecimal;

public record CapFinalScoreParams(
    BigDecimal maxScore
) implements ScoringRuleActionParams {
    public CapFinalScoreParams {
        if (maxScore == null || maxScore.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Điểm tối đa không được để trống hoặc dưới 0");
        }
    }
}

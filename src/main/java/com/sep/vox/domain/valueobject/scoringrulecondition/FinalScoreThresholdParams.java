package com.sep.vox.domain.valueobject.scoringrulecondition;

import java.math.BigDecimal;

public record FinalScoreThresholdParams(BigDecimal score) implements ScoringRuleConditionParams {
    public FinalScoreThresholdParams {
        if (score == null || score.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Điểm số không được để trống hoặc dưới 0");
        }
    }
}

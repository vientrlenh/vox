package com.sep.vox.domain.valueobject.scoringrulecondition;

import java.math.BigDecimal;

public record FinalScoreThresholdParams(BigDecimal score) implements ScoringRuleConditionParams {
    public FinalScoreThresholdParams {
        if (score == null || score.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("The score must not be blank or below 0");
        }
    }
}

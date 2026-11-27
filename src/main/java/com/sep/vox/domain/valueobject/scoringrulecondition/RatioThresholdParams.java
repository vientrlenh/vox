package com.sep.vox.domain.valueobject.scoringrulecondition;

import java.math.BigDecimal;

public record RatioThresholdParams(BigDecimal ratio) implements ScoringRuleConditionParams {
    public RatioThresholdParams {
        if (ratio == null
                || ratio.compareTo(BigDecimal.ZERO) < 0
                || ratio.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("The ratio must be between 0 and 1");
        }
    }
}

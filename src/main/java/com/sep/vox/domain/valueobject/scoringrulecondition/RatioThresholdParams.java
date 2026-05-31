package com.sep.vox.domain.valueobject.scoringrulecondition;

import java.math.BigDecimal;

public record RatioThresholdParams(BigDecimal ratio) implements ScoringRuleConditionParams {
    public RatioThresholdParams {
        if (ratio == null
                || ratio.compareTo(BigDecimal.ZERO) < 0
                || ratio.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Ratio bắt buộc trong khoảng 0 đến 1");
        }
    }
}

package com.sep.vox.domain.valueobject.scoringrulecondition;

import java.math.BigDecimal;

public record ConfidenceThresholdParams(BigDecimal confidence) implements ScoringRuleConditionParams {
    public ConfidenceThresholdParams {
        if (confidence == null
                || confidence.compareTo(BigDecimal.ZERO) < 0
                || confidence.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Độ tự tin bắt buộc trong khoảng 0 đến 1");
        }
    }
}

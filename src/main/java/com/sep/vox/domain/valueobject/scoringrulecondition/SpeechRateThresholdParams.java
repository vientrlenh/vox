package com.sep.vox.domain.valueobject.scoringrulecondition;

import java.math.BigDecimal;

public record SpeechRateThresholdParams(BigDecimal wordsPerMinute) implements ScoringRuleConditionParams {
    public SpeechRateThresholdParams {
        if (wordsPerMinute == null || wordsPerMinute.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("The words per minute must not be less than or equal to 0 or blank");
        }
    }
}

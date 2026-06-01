package com.sep.vox.domain.valueobject.scoringrulecondition;

import java.math.BigDecimal;

public record SpeechRateThresholdParams(BigDecimal wordsPerMinute) implements ScoringRuleConditionParams {
    public SpeechRateThresholdParams {
        if (wordsPerMinute == null || wordsPerMinute.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số từ vựng mỗi phút không được nhỏ hơn hoặc bằng 0 hoặc không được phép để trống");
        }
    }
}

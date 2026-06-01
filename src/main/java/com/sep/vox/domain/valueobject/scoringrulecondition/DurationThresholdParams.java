package com.sep.vox.domain.valueobject.scoringrulecondition;

public record DurationThresholdParams(int seconds) implements ScoringRuleConditionParams {
    public DurationThresholdParams {
        if (seconds <= 0) {
            throw new IllegalArgumentException("Thời lượng không được dưới hoặc bằng 0");
        }
    }
}

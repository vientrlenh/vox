package com.sep.vox.domain.valueobject.scoringrulecondition;

public record DurationThresholdParams(int seconds) implements ScoringRuleConditionParams {
    public DurationThresholdParams {
        if (seconds <= 0) {
            throw new IllegalArgumentException("The duration must not be less than or equal to 0");
        }
    }
}

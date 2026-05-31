package com.sep.vox.domain.valueobject.scoringrulecondition;

public record WordCountThresholdParams(int words) implements ScoringRuleConditionParams {
    public WordCountThresholdParams {
        if (words <= 0) {
            throw new IllegalArgumentException("Số từ không được nhỏ hơn hoặc bằng 0");
        }
    }
}

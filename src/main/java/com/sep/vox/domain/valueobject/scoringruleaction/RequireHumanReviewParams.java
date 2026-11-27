package com.sep.vox.domain.valueobject.scoringruleaction;

public record RequireHumanReviewParams(
    String reasonCode
) implements ScoringRuleActionParams {
    public RequireHumanReviewParams {
        if (reasonCode == null || reasonCode.isBlank()) {
            throw new IllegalArgumentException("The reason code must not be blank");
        }
    }
}

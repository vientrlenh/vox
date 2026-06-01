package com.sep.vox.domain.valueobject.scoringruleaction;

public record RequireHumanReviewParams(
    String reasonCode
) implements ScoringRuleActionParams {
    public RequireHumanReviewParams {
        if (reasonCode == null || reasonCode.isBlank()) {
            throw new IllegalArgumentException("Reason code không được để trống");
        }
    }
}

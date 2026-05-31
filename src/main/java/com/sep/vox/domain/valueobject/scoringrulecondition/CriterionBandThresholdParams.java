package com.sep.vox.domain.valueobject.scoringrulecondition;

public record CriterionBandThresholdParams(
    String criterionCode,
    String bandCode
) implements ScoringRuleConditionParams {
    public CriterionBandThresholdParams {
        if (criterionCode == null || criterionCode.isBlank()) {
            throw new IllegalArgumentException("Code của tiêu chí không được để trống");
        }
        if (bandCode == null || bandCode.isBlank()) {
            throw new IllegalArgumentException("Band code không được để trống");
        }
    }
}

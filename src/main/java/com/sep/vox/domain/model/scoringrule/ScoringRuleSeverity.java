package com.sep.vox.domain.model.scoringrule;

public enum ScoringRuleSeverity {
    INFO, 
    WARNING, 
    BLOCKING;

    public static ScoringRuleSeverity from(String value) {
        return value == null ? null : ScoringRuleSeverity.valueOf(value);
    }

    public static String value(ScoringRuleSeverity value) {
        return value == null ? null : value.name();
    }
}

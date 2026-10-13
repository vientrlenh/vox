package com.sep.vox.domain.model.assessmentpolicy;

public enum AssessmentPolicyStrictness {
    LENIENT,
    STANDARD, 
    STRICT;

    public static AssessmentPolicyStrictness from(String value) {
        return value == null ? null : AssessmentPolicyStrictness.valueOf(value);
    }

    public static String value(AssessmentPolicyStrictness value) {
        return value == null ? null : value.name();
    }
}

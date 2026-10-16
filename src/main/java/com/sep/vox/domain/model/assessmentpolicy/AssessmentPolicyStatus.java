package com.sep.vox.domain.model.assessmentpolicy;

public enum AssessmentPolicyStatus {
    DRAFT, 
    PUBLISHED, 
    ARCHIVED;

    public static AssessmentPolicyStatus from(String value) {
        return value == null ? null : AssessmentPolicyStatus.valueOf(value);
    }

    public static String value(AssessmentPolicyStatus value) {
        return value == null ? null : value.name();
    }
}

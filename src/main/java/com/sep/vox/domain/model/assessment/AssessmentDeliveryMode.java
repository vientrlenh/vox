package com.sep.vox.domain.model.assessment;

public enum AssessmentDeliveryMode {
    STUDENT_DEVICE,
    LAB; 

    public static AssessmentDeliveryMode from(String value) {
        return value == null ? null : AssessmentDeliveryMode.valueOf(value);
    }

    public static String value(AssessmentDeliveryMode mode) {
        return mode == null ? null : mode.name();
    }

    public static AssessmentDeliveryMode parse(String value) {
        try {
            return AssessmentDeliveryMode.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Assessment taking method is not valid");
        }
    }

    public static AssessmentDeliveryMode parseOrDefault(String value, AssessmentDeliveryMode fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return parse(value);
    }
}

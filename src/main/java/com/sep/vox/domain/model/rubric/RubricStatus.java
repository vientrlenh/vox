package com.sep.vox.domain.model.rubric;

public enum RubricStatus {
    DRAFT, 
    PUBLISHED, 
    ARCHIVED;

    public static RubricStatus from(String value) {
        return value == null ? null : RubricStatus.valueOf(value);
    }

    public static String value(RubricStatus value) {
        return value == null ? null : value.name();
    }
}

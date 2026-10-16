package com.sep.vox.domain.model.rubric;

public enum RubricOwnerType {
    SYSTEM,
    SCHOOL;

    public static RubricOwnerType from(String value) {
        return value == null ? null : RubricOwnerType.valueOf(value);
    }

    public static String value(RubricOwnerType value) {
        return value == null ? null : value.name();
    }
}

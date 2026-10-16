package com.sep.vox.domain.model.framework;

public enum FrameworkStatus {
    INACTIVE, 
    ACTIVE, 
    ARCHIVED;

    public static FrameworkStatus from(String value) {
        return value == null ? null : FrameworkStatus.valueOf(value);
    }

    public static String value(FrameworkStatus value) {
        return value == null ? null : value.name();
    }
}

package com.sep.vox.domain.model.framework;

public enum FrameworkVersionStatus {
    DRAFT, 
    PUBLISHED, 
    ARCHIVED;

    public static FrameworkVersionStatus from(String value) {
        return value == null ? null : FrameworkVersionStatus.valueOf(value);
    }

    public static String value(FrameworkVersionStatus value) {
        return value == null ? null : value.name();
    }
}

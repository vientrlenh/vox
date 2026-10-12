package com.sep.vox.domain.model.gradelevel;

public enum GradeLevelStatus {
    ACTIVE,
    INACTIVE; 

    public static GradeLevelStatus from(String status) {
        return status == null ? null : GradeLevelStatus.valueOf(status);
    }

    public static String value(GradeLevelStatus status) {
        return status == null ? null : status.name();
    }
}

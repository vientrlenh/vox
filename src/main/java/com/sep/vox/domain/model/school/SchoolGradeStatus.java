package com.sep.vox.domain.model.school;

public enum SchoolGradeStatus {
    INACTIVE,
    ACTIVE,
    ARCHIVED; 

    public static SchoolGradeStatus from(String status) {
        return status == null ? null : SchoolGradeStatus.valueOf(status);
    }

    public static String value(SchoolGradeStatus status) {
        return status == null ? null : status.name();
    }
}

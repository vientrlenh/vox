package com.sep.vox.domain.model.school;

public enum SchoolClassStatus {
    INACTIVE, 
    ACTIVE, 
    ARCHIVED; 

    public static SchoolClassStatus from(String status) {
        return status == null ? null : SchoolClassStatus.valueOf(status);
    }

    public static String value(SchoolClassStatus status) {
        return status == null ? null : status.name();
    }
}

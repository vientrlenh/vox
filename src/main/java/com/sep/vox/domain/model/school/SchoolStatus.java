package com.sep.vox.domain.model.school;

public enum SchoolStatus {
    INACTIVE, 
    ACTIVE; 

    public static SchoolStatus from(String status) {
        return status == null ? null : SchoolStatus.valueOf(status);
    }

    public static String value(SchoolStatus status) {
        return status == null ? null : status.name();
    }
}

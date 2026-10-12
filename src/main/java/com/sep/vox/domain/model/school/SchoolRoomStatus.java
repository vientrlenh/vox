package com.sep.vox.domain.model.school;

public enum SchoolRoomStatus {
    INACTIVE, 
    ACTIVE, 
    ARCHIVED;

    public static SchoolRoomStatus from(String status) {
        return status == null ? null : SchoolRoomStatus.valueOf(status);
    }

    public static String value(SchoolRoomStatus status) {
        return status == null ? null : status.name();
    }
}

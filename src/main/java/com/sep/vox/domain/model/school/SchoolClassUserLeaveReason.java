package com.sep.vox.domain.model.school;

public enum SchoolClassUserLeaveReason {
    MOVED, 
    LEFT_SCHOOL, 
    YEAR_CLOSED, 
    REMOVED;

    public static SchoolClassUserLeaveReason from(String reason) {
        return reason == null ? null : SchoolClassUserLeaveReason.valueOf(reason);
    }

    public static String value(SchoolClassUserLeaveReason reason) {
        return reason == null ? null : reason.name();
    }
}

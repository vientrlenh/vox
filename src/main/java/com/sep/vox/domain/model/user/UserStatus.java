package com.sep.vox.domain.model.user;

public enum UserStatus {
    ACTIVE,
    INACTIVE,
    LOCKED,
    DISABLED;

    public static UserStatus from(String status) {
        return status == null ? null : UserStatus.valueOf(status);
    }

    public static String value(UserStatus status) {
        return status == null ? null : status.name();
    }
}

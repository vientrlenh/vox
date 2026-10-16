package com.sep.vox.domain.model.school;

public enum SchoolDirectoryOrigin {
    OFFICIAL_IMPORT, 
    ADMIN_CREATED,
    USER_SUBMITTED;

    public static SchoolDirectoryOrigin from(String value) {
        return value == null ? null : SchoolDirectoryOrigin.valueOf(value);
    }

    public static String value(SchoolDirectoryOrigin value) {
        return value == null ? null : value.name();
    }
}

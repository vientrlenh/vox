package com.sep.vox.application.common.permission;

import java.util.List;

public enum UserRole {
    SYSTEM_ADMIN,
    TEACHER,
    STUDENT,
    SCHOOL_ADMIN;

    public static UserRole fromRoleCodes(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new IllegalStateException("User has no roles assigned");
        }
        if (roleCodes.contains("SYSTEM_ADMIN")) return SYSTEM_ADMIN;
        if (roleCodes.contains("SCHOOL_ADMIN")) return SCHOOL_ADMIN;
        if (roleCodes.contains("TEACHER")) return TEACHER;
        if (roleCodes.contains("STUDENT")) return STUDENT;
        throw new IllegalStateException("Unknown role codes: " + roleCodes);
    }
}

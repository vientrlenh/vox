package com.sep.vox.domain.model.user;

public enum UserRole {
    SYSTEM_ADMIN,
    SCHOOL_MEMBER, 
    LEARNER;

    public static UserRole from(String role) {
        return role == null ? null : UserRole.valueOf(role);
    }

    public static String value(UserRole role) {
        return role == null ? null : role.name();
    }
}

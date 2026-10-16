package com.sep.vox.domain.model.school;

public enum SchoolUserRole {
    SCHOOL_ADMIN, 
    TEACHER, 
    STUDENT;

    public static SchoolUserRole from(String role) {
        return role == null ? null : SchoolUserRole.valueOf(role);
    }

    public static String value(SchoolUserRole role) {
        return role == null ? null : role.name();
    }
}

package com.sep.vox.domain.model.school;

public enum SchoolStudentEnrollmentStatus {
    ENROLLED, 
    PROMOTED, 
    RETAINED, 
    GRADUATED, 
    LEFT; 

    public static SchoolStudentEnrollmentStatus from(String status) {
        return status == null ? null : SchoolStudentEnrollmentStatus.valueOf(status);
    }

    public static String value(SchoolStudentEnrollmentStatus status) {
        return status == null ? null : status.name();
    }
}

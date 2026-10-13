package com.sep.vox.domain.model.school;

public enum SchoolAcademicYearStatus {
    PLANNING,
    ACTIVE, 
    CLOSED; 

    public static SchoolAcademicYearStatus from(String status) {
        return status == null ? null : SchoolAcademicYearStatus.valueOf(status);
    }

    public static String value(SchoolAcademicYearStatus status) {
        return status == null ? null : status.name();
    }
}

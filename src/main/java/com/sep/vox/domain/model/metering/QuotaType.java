package com.sep.vox.domain.model.metering;

import com.sep.vox.domain.model.school.SchoolUserRole;

public enum QuotaType {
    /** Chấm thi -- mọi AssessmentKind (CENTRALIZED lẫn CLASS_TEST) đều trừ vào đây. */
    ASSESSMENT,
    /** Học sinh tự luyện nói. */
    PRACTICE;

    public SchoolUserRole allocationRole() {
        return switch (this) {
            case ASSESSMENT -> SchoolUserRole.TEACHER;
            case PRACTICE -> SchoolUserRole.STUDENT;
        };
    }

    public String allocationRoleCode() {
        return allocationRole().name();
    }
}

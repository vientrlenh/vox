package com.sep.vox.domain.model.gradelevel;

// Giữ nguyên ACTIVE/INACTIVE như SchoolGradeLevelStatus cũ -- check constraint
// chk_school_grade_levels_status_valid trong DB và type SchoolGradeLevelStatus phía web
// (vox-client-web/src/features/grades/types.ts) đều đang chốt đúng 2 giá trị này.
public enum GradeLevelStatus {
    ACTIVE,
    INACTIVE
}

package com.sep.vox.domain.model.gradelevel;

// Keeps ACTIVE/INACTIVE as in the old SchoolGradeLevelStatus -- the check constraint
// chk_school_grade_levels_status_valid in the DB and the SchoolGradeLevelStatus type on the web side
// (vox-client-web/src/features/grades/types.ts) both pin exactly these 2 values.
public enum GradeLevelStatus {
    ACTIVE,
    INACTIVE
}

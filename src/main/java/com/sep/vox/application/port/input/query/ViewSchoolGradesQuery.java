package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.domain.model.school.SchoolGradeStatus;

public record ViewSchoolGradesQuery(
        UUID schoolId,
        UUID gradeLevelId,
        SchoolGradeStatus status,
        int page,
        int size
) {
}
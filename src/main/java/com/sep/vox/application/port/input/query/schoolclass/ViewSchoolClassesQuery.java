package com.sep.vox.application.port.input.query.schoolclass;

import java.util.UUID;

import com.sep.vox.domain.model.school.SchoolClassStatus;

public record ViewSchoolClassesQuery(
    int page,
    int size,
    String search,
    SchoolClassStatus status,
    UUID languageId,
    UUID schoolGradeId
) {
}

package com.sep.vox.application.port.input.query.schoolclass;

import java.util.UUID;

import com.sep.vox.domain.model.school.SchoolClassStatus;

public record ViewMySchoolClassesQuery(
    UUID schoolId,
    SchoolClassStatus status,
    int page,
    int size
) {

}

package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.domain.model.school.SchoolClassStatus;

public record ViewMyClassesQuery(
    UUID schoolId,
    String search,
    SchoolClassStatus status,
    int page,
    int size
) {

}

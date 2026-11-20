package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewSchoolClassesQuery(
    int page,
    int size,
    String search,
    String status,
    UUID languageId,
    UUID schoolGradeId
) {
}

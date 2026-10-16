package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewAssessmentBlueprintsQuery(
    UUID schoolId,
    Boolean isActive,
    UUID languageId,
    String assessmentKind,
    String keyword,
    int page,
    int size
) {
}

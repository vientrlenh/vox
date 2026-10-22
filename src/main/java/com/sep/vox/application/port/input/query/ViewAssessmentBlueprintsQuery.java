package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.application.shared.AppPageRequest;

public record ViewAssessmentBlueprintsQuery(
    UUID schoolId,
    Boolean isActive,
    UUID languageId,
    String assessmentKind,
    String keyword,
    AppPageRequest pageRequest
) {
}

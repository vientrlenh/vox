package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.application.shared.AppPageRequest;

public record ViewSchoolClassesQuery(
    AppPageRequest pageRequest,
    String search,
    String status,
    UUID languageId,
    UUID schoolGradeId
) {
}

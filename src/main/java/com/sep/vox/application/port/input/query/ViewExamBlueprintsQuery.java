package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewExamBlueprintsQuery(
    UUID schoolId,
    Boolean isActive,
    UUID languageId,
    String examKind,
    String keyword,
    int page,
    int size
) {
}

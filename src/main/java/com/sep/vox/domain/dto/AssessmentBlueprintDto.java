package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.UUID;

public record AssessmentBlueprintDto(
    UUID id,
    UUID schoolId,
    UUID languageId,
    UUID gradeLevelId,
    String code,
    String name,
    String description,
    boolean isActive,
    Instant createdAt,
    Instant updatedAt,
    UUID createdBy,
    UUID updatedBy
) {
}

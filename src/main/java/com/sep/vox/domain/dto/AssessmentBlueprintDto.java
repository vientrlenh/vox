package com.sep.vox.domain.dto;

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
    String createdAt,
    String updatedAt,
    UUID createdBy,
    UUID updatedBy
) {
}

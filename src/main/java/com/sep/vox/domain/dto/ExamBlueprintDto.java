package com.sep.vox.domain.dto;

import java.util.UUID;

public record ExamBlueprintDto(
    UUID id,
    UUID schoolId,
    UUID languageId,
    UUID schoolGradeLevelId,
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

package com.sep.vox.domain.dto;

import java.util.UUID;

public record ExamBlueprintVersionDto(
    UUID id,
    UUID blueprintId,
    int version,
    String code,
    String description,
    String status,
    Integer totalTimeLimitSeconds,
    String effectiveFrom,
    String effectiveTo,
    String createdAt,
    String updatedAt,
    UUID createdBy,
    UUID updatedBy
) {
}

package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ExamBlueprintSectionDto(
    UUID id,
    UUID blueprintVersionId,
    int order,
    String title,
    String instruction,
    Integer sectionTimeLimitSeconds,
    BigDecimal sectionWeight,
    String createdAt,
    String updatedAt,
    UUID createdBy,
    UUID updatedBy
) {
}

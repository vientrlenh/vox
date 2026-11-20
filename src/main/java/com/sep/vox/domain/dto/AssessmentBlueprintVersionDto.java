package com.sep.vox.domain.dto;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import java.time.Instant;
import java.util.UUID;

public record AssessmentBlueprintVersionDto(
    UUID id,
    UUID blueprintId,
    int version,
    String code,
    String description,
    AssessmentBlueprintVersionStatus status,
    Integer totalTimeLimitSeconds,
    Instant effectiveFrom,
    Instant effectiveTo,
    Instant createdAt,
    Instant updatedAt,
    UUID createdBy,
    UUID updatedBy
) {
}

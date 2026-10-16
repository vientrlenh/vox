package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;

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

    public static AssessmentBlueprintVersionDto toDto(AssessmentBlueprintVersion assessmentBlueprintVersion) {
        return new AssessmentBlueprintVersionDto(
            assessmentBlueprintVersion.getId(),
            assessmentBlueprintVersion.getBlueprintId(),
            assessmentBlueprintVersion.getVersion(),
            assessmentBlueprintVersion.getCode(),
            assessmentBlueprintVersion.getDescription(),
            assessmentBlueprintVersion.getStatus(),
            assessmentBlueprintVersion.getTotalTimeLimitSeconds(),
            assessmentBlueprintVersion.getEffectiveFrom(),
            assessmentBlueprintVersion.getEffectiveTo(),
            assessmentBlueprintVersion.getCreatedAt(),
            assessmentBlueprintVersion.getUpdatedAt(),
            assessmentBlueprintVersion.getCreatedBy(),
            assessmentBlueprintVersion.getUpdatedBy()
        );
    }

    public static List<AssessmentBlueprintVersionDto> toDtoList(List<AssessmentBlueprintVersion> domains) {
        return domains.stream()
            .map(AssessmentBlueprintVersionDto::toDto)
            .toList();
    }
}

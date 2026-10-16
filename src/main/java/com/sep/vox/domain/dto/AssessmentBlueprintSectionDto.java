package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;

public record AssessmentBlueprintSectionDto(
    UUID id,
    UUID blueprintVersionId,
    int order,
    String title,
    String instruction,
    Integer sectionTimeLimitSeconds,
    BigDecimal sectionWeight,
    Instant createdAt,
    Instant updatedAt,
    UUID createdBy,
    UUID updatedBy
) {

    public static AssessmentBlueprintSectionDto toDto(AssessmentBlueprintSection assessmentBlueprintSection) {
        return new AssessmentBlueprintSectionDto(
            assessmentBlueprintSection.getId(),
            assessmentBlueprintSection.getBlueprintVersionId(),
            assessmentBlueprintSection.getOrder(),
            assessmentBlueprintSection.getTitle(),
            assessmentBlueprintSection.getInstruction(),
            assessmentBlueprintSection.getSectionTimeLimitSeconds(),
            assessmentBlueprintSection.getSectionWeight(),
            assessmentBlueprintSection.getCreatedAt(),
            assessmentBlueprintSection.getUpdatedAt(),
            assessmentBlueprintSection.getCreatedBy(),
            assessmentBlueprintSection.getUpdatedBy()
        );
    }

    public static List<AssessmentBlueprintSectionDto> toDtoList(List<AssessmentBlueprintSection> domains) {
        return domains.stream()
            .map(AssessmentBlueprintSectionDto::toDto)
            .toList();
    }
}

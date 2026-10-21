package com.sep.vox.domain.dto.assessment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import com.sep.vox.domain.shared.PageResult;

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

    public static AssessmentBlueprintDto toDto(AssessmentBlueprint assessmentBlueprint) {
        return new AssessmentBlueprintDto(
            assessmentBlueprint.getId(),
            assessmentBlueprint.getSchoolId(),
            assessmentBlueprint.getLanguageId(),
            assessmentBlueprint.getGradeLevelId(),
            assessmentBlueprint.getCode(),
            assessmentBlueprint.getName(),
            assessmentBlueprint.getDescription(),
            assessmentBlueprint.isActive(),
            assessmentBlueprint.getCreatedAt(),
            assessmentBlueprint.getUpdatedAt(),
            assessmentBlueprint.getCreatedBy(),
            assessmentBlueprint.getUpdatedBy()
        );
    }

    public static List<AssessmentBlueprintDto> toDtoList(List<AssessmentBlueprint> domains) {
        return domains.stream()
            .map(AssessmentBlueprintDto::toDto)
            .toList();
    }

    public static PageResult<AssessmentBlueprintDto> toDtoPage(PageResult<AssessmentBlueprint> page) {
        return new PageResult<>(
            toDtoList(page.content()),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }
}

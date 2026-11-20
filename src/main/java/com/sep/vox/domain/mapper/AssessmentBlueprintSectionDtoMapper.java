package com.sep.vox.domain.mapper;

import java.util.List;

import com.sep.vox.domain.dto.AssessmentBlueprintSectionDto;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;

public final class AssessmentBlueprintSectionDtoMapper {

    private AssessmentBlueprintSectionDtoMapper() {
    }

    public static AssessmentBlueprintSectionDto toDto(AssessmentBlueprintSection domain) {
        return new AssessmentBlueprintSectionDto(
            domain.getId(),
            domain.getBlueprintVersionId(),
            domain.getOrder(),
            domain.getTitle(),
            domain.getInstruction(),
            domain.getSectionTimeLimitSeconds(),
            domain.getSectionWeight(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
        );
    }

    public static List<AssessmentBlueprintSectionDto> toDtoList(List<AssessmentBlueprintSection> domains) {
        return domains.stream()
            .map(AssessmentBlueprintSectionDtoMapper::toDto)
            .toList();
    }
}

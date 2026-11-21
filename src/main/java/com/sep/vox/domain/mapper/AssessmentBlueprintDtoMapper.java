package com.sep.vox.domain.mapper;

import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.AssessmentBlueprintDto;
import com.sep.vox.domain.model.assessment.AssessmentBlueprint;

public final class AssessmentBlueprintDtoMapper {

    private AssessmentBlueprintDtoMapper() {
    }

    public static AssessmentBlueprintDto toDto(AssessmentBlueprint domain) {
        return new AssessmentBlueprintDto(
            domain.getId(),
            domain.getSchoolId(),
            domain.getLanguageId(),
            domain.getGradeLevelId(),
            domain.getCode(),
            domain.getName(),
            domain.getDescription(),
            domain.isActive(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.createdByUserId().orElse(null),
            domain.updatedByUserId().orElse(null)
        );
    }

    public static List<AssessmentBlueprintDto> toDtoList(List<AssessmentBlueprint> domains) {
        return domains.stream()
            .map(AssessmentBlueprintDtoMapper::toDto)
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

package com.sep.vox.domain.mapper;

import java.util.List;

import com.sep.vox.domain.dto.AssessmentBlueprintVersionDto;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;

public final class AssessmentBlueprintVersionDtoMapper {

    private AssessmentBlueprintVersionDtoMapper() {
    }

    public static AssessmentBlueprintVersionDto toDto(AssessmentBlueprintVersion domain) {
        return new AssessmentBlueprintVersionDto(
            domain.getId(),
            domain.getBlueprintId(),
            domain.getVersion(),
            domain.getCode(),
            domain.getDescription(),
            domain.getStatus(),
            domain.getTotalTimeLimitSeconds(),
            domain.getEffectiveFrom(),
            domain.getEffectiveTo(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.createdByUserId().orElse(null),
            domain.updatedByUserId().orElse(null)
        );
    }

    public static List<AssessmentBlueprintVersionDto> toDtoList(List<AssessmentBlueprintVersion> domains) {
        return domains.stream()
            .map(AssessmentBlueprintVersionDtoMapper::toDto)
            .toList();
    }
}

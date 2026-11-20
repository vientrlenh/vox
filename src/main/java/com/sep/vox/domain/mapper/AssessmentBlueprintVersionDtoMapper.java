package com.sep.vox.domain.mapper;

import java.time.Instant;
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
            domain.getStatus().name(),
            domain.getTotalTimeLimitSeconds(),
            valueOf(domain.getEffectiveFrom()),
            valueOf(domain.getEffectiveTo()),
            valueOf(domain.getCreatedAt()),
            valueOf(domain.getUpdatedAt()),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
        );
    }

    public static List<AssessmentBlueprintVersionDto> toDtoList(List<AssessmentBlueprintVersion> domains) {
        return domains.stream()
            .map(AssessmentBlueprintVersionDtoMapper::toDto)
            .toList();
    }

    private static String valueOf(Instant value) {
        return value == null ? null : value.toString();
    }
}

package com.sep.vox.domain.mapper;

import java.time.Instant;
import java.util.List;

import com.sep.vox.domain.dto.ExamBlueprintVersionDto;
import com.sep.vox.domain.model.exam.ExamBlueprintVersion;

public final class ExamBlueprintVersionDtoMapper {

    private ExamBlueprintVersionDtoMapper() {
    }

    public static ExamBlueprintVersionDto toDto(ExamBlueprintVersion domain) {
        return new ExamBlueprintVersionDto(
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

    public static List<ExamBlueprintVersionDto> toDtoList(List<ExamBlueprintVersion> domains) {
        return domains.stream()
            .map(ExamBlueprintVersionDtoMapper::toDto)
            .toList();
    }

    private static String valueOf(Instant value) {
        return value == null ? null : value.toString();
    }
}

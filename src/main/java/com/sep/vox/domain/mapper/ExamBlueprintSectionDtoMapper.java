package com.sep.vox.domain.mapper;

import java.time.Instant;
import java.util.List;

import com.sep.vox.domain.dto.ExamBlueprintSectionDto;
import com.sep.vox.domain.model.exam.ExamBlueprintSection;

public final class ExamBlueprintSectionDtoMapper {

    private ExamBlueprintSectionDtoMapper() {
    }

    public static ExamBlueprintSectionDto toDto(ExamBlueprintSection domain) {
        return new ExamBlueprintSectionDto(
            domain.getId(),
            domain.getBlueprintVersionId(),
            domain.getOrder(),
            domain.getTitle(),
            domain.getInstruction(),
            domain.getSectionTimeLimitSeconds(),
            domain.getSectionWeight(),
            valueOf(domain.getCreatedAt()),
            valueOf(domain.getUpdatedAt()),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
        );
    }

    public static List<ExamBlueprintSectionDto> toDtoList(List<ExamBlueprintSection> domains) {
        return domains.stream()
            .map(ExamBlueprintSectionDtoMapper::toDto)
            .toList();
    }

    private static String valueOf(Instant value) {
        return value == null ? null : value.toString();
    }
}

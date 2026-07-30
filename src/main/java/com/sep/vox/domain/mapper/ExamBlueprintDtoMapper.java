package com.sep.vox.domain.mapper;

import java.time.Instant;
import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.ExamBlueprintDto;
import com.sep.vox.domain.model.exam.ExamBlueprint;

public final class ExamBlueprintDtoMapper {

    private ExamBlueprintDtoMapper() {
    }

    public static ExamBlueprintDto toDto(ExamBlueprint domain) {
        return new ExamBlueprintDto(
            domain.getId(),
            domain.getSchoolId(),
            domain.getLanguageId(),
            domain.getSchoolGradeLevelId(),
            domain.getCode(),
            domain.getName(),
            domain.getDescription(),
            domain.isActive(),
            valueOf(domain.getCreatedAt()),
            valueOf(domain.getUpdatedAt()),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
        );
    }

    public static List<ExamBlueprintDto> toDtoList(List<ExamBlueprint> domains) {
        return domains.stream()
            .map(ExamBlueprintDtoMapper::toDto)
            .toList();
    }

    public static PageResult<ExamBlueprintDto> toDtoPage(PageResult<ExamBlueprint> page) {
        return new PageResult<>(
            toDtoList(page.content()),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }

    private static String valueOf(Instant value) {
        return value == null ? null : value.toString();
    }
}

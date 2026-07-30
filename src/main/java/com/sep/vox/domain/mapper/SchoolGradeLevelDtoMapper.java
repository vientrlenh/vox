package com.sep.vox.domain.mapper;

import java.time.Instant;
import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolGradeLevelDto;
import com.sep.vox.domain.model.school.SchoolGradeLevel;
import com.sep.vox.domain.model.school.SchoolGradeLevelStatus;

public final class SchoolGradeLevelDtoMapper {

    private SchoolGradeLevelDtoMapper() {
    }

    public static SchoolGradeLevelDto toDto(SchoolGradeLevel gradeLevel) {
        return new SchoolGradeLevelDto(
            gradeLevel.getId(),
            gradeLevel.getSchoolId(),
            gradeLevel.getCode(),
            gradeLevel.getName(),
            gradeLevel.getDescription(),
            gradeLevel.getOrder(),
            valueOf(gradeLevel.getStatus()),
            valueOf(gradeLevel.getCreatedAt()),
            valueOf(gradeLevel.getUpdatedAt())
        );
    }

    public static List<SchoolGradeLevelDto> toDtoList(List<SchoolGradeLevel> list) {
        return list.stream()
            .map(SchoolGradeLevelDtoMapper::toDto)
            .toList();
    }

    public static PageResult<SchoolGradeLevelDto> toDtoPage(PageResult<SchoolGradeLevel> page) {
        return new PageResult<>(
            toDtoList(page.content()),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }

    private static String valueOf(SchoolGradeLevelStatus status) {
        return status == null ? null : status.name();
    }

    private static String valueOf(Instant dateTime) {
        return dateTime == null ? null : dateTime.toString();
    }
}

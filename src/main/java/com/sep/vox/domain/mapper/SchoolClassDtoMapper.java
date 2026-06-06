package com.sep.vox.domain.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.valueobject.ClassCode;

public final class SchoolClassDtoMapper {

    public static SchoolClassDto toDto(SchoolClass schoolClass) {
        return new SchoolClassDto(
            schoolClass.getId(),
            schoolClass.getSchoolId(),
            schoolClass.getLanguageId(),
            schoolClass.getSchoolGradeId(),
            valueOf(schoolClass.getCode()),
            schoolClass.getName(),
            schoolClass.getDescription(),
            valueOf(schoolClass.getStatus()),
            valueOf(schoolClass.getCreatedAt()),
            valueOf(schoolClass.getUpdatedAt())
        );
    }

    public static List<SchoolClassDto> toDtoList(List<SchoolClass> list) {
        return list.stream()
            .map(SchoolClassDtoMapper::toDto)
            .toList();
    }

    public static PageResult<SchoolClassDto> toDtoPage(PageResult<SchoolClass> page) {
        return new PageResult<>(
            toDtoList(page.content()),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }

    private static String valueOf(ClassCode code) {
        return code == null ? null : code.value();
    }

    private static String valueOf(SchoolClassStatus status) {
        return status == null ? null : status.name();
    }

    private static String valueOf(OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.toString();
    }
}

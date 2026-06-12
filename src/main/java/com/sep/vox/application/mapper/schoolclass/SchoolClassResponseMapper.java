package com.sep.vox.application.mapper.schoolclass;

import java.time.OffsetDateTime;
import java.util.List;

import com.sep.vox.application.response.input.schoolclass.SchoolClassResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.valueobject.ClassCode;

public final class SchoolClassResponseMapper {

    private SchoolClassResponseMapper() {
    }

    public static SchoolClassResponse toResponse(SchoolClass schoolClass) {
        return new SchoolClassResponse(
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

    public static List<SchoolClassResponse> toResponseList(List<SchoolClass> list) {
        return list.stream()
            .map(SchoolClassResponseMapper::toResponse)
            .toList();
    }

    public static PageResult<SchoolClassResponse> toResponsePage(PageResult<SchoolClass> page) {
        return new PageResult<>(
            toResponseList(page.content()),
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

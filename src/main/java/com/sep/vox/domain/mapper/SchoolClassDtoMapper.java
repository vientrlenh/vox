package com.sep.vox.domain.mapper;

import java.time.OffsetDateTime;

import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.model.schoolclass.SchoolClass;
import com.sep.vox.domain.model.schoolclass.SchoolClassStatus;
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
            schoolClass.getTargetSchoolLevelVersionId(),
            valueOf(schoolClass.getStatus()),
            valueOf(schoolClass.getCreatedAt()),
            valueOf(schoolClass.getUpdatedAt()),
            schoolClass.getCreatedBy(),
            schoolClass.getUpdatedBy()
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

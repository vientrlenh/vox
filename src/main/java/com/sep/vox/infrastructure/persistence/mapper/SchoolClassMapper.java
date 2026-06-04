package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.valueobject.ClassCode;
import com.sep.vox.infrastructure.persistence.entity.SchoolClassJpaEntity;

public final class SchoolClassMapper {

    public static SchoolClass toDomain(SchoolClassJpaEntity jpa) {
        return new SchoolClass(
            jpa.getId(),
            jpa.getSchoolId(),
            jpa.getLanguageId(),
            jpa.getSchoolGradeId(),
            new ClassCode(jpa.getCode()),
            jpa.getName(),
            jpa.getDescription(),
            statusFromString(jpa.getStatus()),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static SchoolClassJpaEntity toJpa(SchoolClass schoolClass) {
        return new SchoolClassJpaEntity(
            schoolClass.getId(),
            schoolClass.getSchoolId(),
            schoolClass.getLanguageId(),
            schoolClass.getSchoolGradeId(),
            valueOf(schoolClass.getCode()),
            schoolClass.getName(),
            schoolClass.getDescription(),
            valueOf(schoolClass.getStatus()),
            schoolClass.getCreatedAt(),
            schoolClass.getUpdatedAt(),
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

    private static SchoolClassStatus statusFromString(String status) {
        return status == null ? null : SchoolClassStatus.valueOf(status);
    }
}

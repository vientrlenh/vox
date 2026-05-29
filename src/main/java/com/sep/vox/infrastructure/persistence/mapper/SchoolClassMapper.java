package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.schoolclass.SchoolClass;
import com.sep.vox.domain.valueobject.ClassCode;
import com.sep.vox.infrastructure.persistence.entity.SchoolClassJpaEntity;

public final class SchoolClassMapper {

    public static SchoolClass toDomain(SchoolClassJpaEntity jpa) {
        return new SchoolClass(
            jpa.getId(),
            jpa.getSchoolId(),
            jpa.getLanguageId(),
            new ClassCode(jpa.getCode()),
            jpa.getName(),
            jpa.getDescription(),
            jpa.getLevelId(),
            jpa.getStartDate(),
            jpa.getEndDate(),
            jpa.isActive(),
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
            valueOf(schoolClass.getCode()),
            schoolClass.getName(),
            schoolClass.getDescription(),
            schoolClass.getLevelId(),
            schoolClass.getStartDate(),
            schoolClass.getEndDate(),
            schoolClass.isActive(),
            schoolClass.getCreatedAt(),
            schoolClass.getUpdatedAt(),
            schoolClass.getCreatedBy(),
            schoolClass.getUpdatedBy()
        );
    }

    private static String valueOf(ClassCode code) {
        return code == null ? null : code.value();
    }
}

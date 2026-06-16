package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.school.SchoolGradeLevel;
import com.sep.vox.domain.model.school.SchoolGradeLevelStatus;
import com.sep.vox.infrastructure.persistence.entity.SchoolGradeLevelJpaEntity;

public final class SchoolGradeLevelMapper {

    private SchoolGradeLevelMapper() {}

    public static SchoolGradeLevel toDomain(SchoolGradeLevelJpaEntity jpa) {
        return new SchoolGradeLevel(
            jpa.getId(),
            jpa.getSchoolId(),
            jpa.getCode(),
            jpa.getName(),
            jpa.getDescription(),
            jpa.getOrder(),
            statusFromString(jpa.getStatus()),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static SchoolGradeLevelJpaEntity toJpa(SchoolGradeLevel level) {
        return new SchoolGradeLevelJpaEntity(
            level.getId(),
            level.getSchoolId(),
            level.getCode(),
            level.getName(),
            level.getDescription(),
            level.getOrder(),
            valueOf(level.getStatus()),
            level.getCreatedAt(),
            level.getUpdatedAt(),
            level.getCreatedBy(),
            level.getUpdatedBy()
        );
    }

    private static SchoolGradeLevelStatus statusFromString(String status) {
        return status == null ? null : SchoolGradeLevelStatus.valueOf(status);
    }

    private static String valueOf(SchoolGradeLevelStatus status) {
        return status == null ? null : status.name();
    }
}

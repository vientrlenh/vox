package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;
import com.sep.vox.infrastructure.persistence.entity.GradeLevelJpaEntity;

public final class GradeLevelMapper {

    private GradeLevelMapper() {}

    public static GradeLevel toDomain(GradeLevelJpaEntity jpa) {
        return new GradeLevel(
            jpa.getId(),
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

    public static GradeLevelJpaEntity toJpa(GradeLevel level) {
        return new GradeLevelJpaEntity(
            level.getId(),
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

    private static GradeLevelStatus statusFromString(String status) {
        return status == null ? null : GradeLevelStatus.valueOf(status);
    }

    private static String valueOf(GradeLevelStatus status) {
        return status == null ? null : status.name();
    }
}

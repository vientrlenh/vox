package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;
import com.sep.vox.infrastructure.persistence.entity.gradelevel.GradeLevelJpaEntity;

public final class GradeLevelMapper {

    private GradeLevelMapper() {}

    public static GradeLevel toDomain(GradeLevelJpaEntity jpa) {
        return GradeLevel.builder()
            .id(jpa.getId())
            .code(jpa.getCode())
            .name(jpa.getName())
            .description(jpa.getDescription())
            .order(jpa.getOrder())
            .status(statusFromString(jpa.getStatus()))
            .createdAt(jpa.getCreatedAt())
            .updatedAt(jpa.getUpdatedAt())
            .createdBy(jpa.getCreatedBy())
            .updatedBy(jpa.getUpdatedBy())
            .build();
    }

    public static GradeLevelJpaEntity toJpa(GradeLevel level) {
        return GradeLevelJpaEntity.builder()
            .id(level.getId())
            .code(level.getCode())
            .name(level.getName())
            .description(level.getDescription())
            .order(level.getOrder())
            .status(valueOf(level.getStatus()))
            .createdAt(level.getCreatedAt())
            .updatedAt(level.getUpdatedAt())
            .createdBy(level.getCreatedBy())
            .updatedBy(level.getUpdatedBy())
            .build();
    }

    private static GradeLevelStatus statusFromString(String status) {
        return status == null ? null : GradeLevelStatus.valueOf(status);
    }

    private static String valueOf(GradeLevelStatus status) {
        return status == null ? null : status.name();
    }
}

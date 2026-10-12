package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;
import com.sep.vox.domain.valueobject.PositiveInteger;
import com.sep.vox.infrastructure.persistence.entity.GradeLevelJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
public final class GradeLevelMapper {

    private GradeLevelMapper() {}

    public static GradeLevel toDomain(GradeLevelJpaEntity jpa) {
        try {
            return GradeLevel.builder()
                .id(jpa.getId())
                .name(jpa.getName())
                .description(jpa.getDescription())
                .order(PositiveInteger.from(jpa.getOrder()))
                .status(GradeLevelStatus.from(jpa.getStatus()))
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .createdBy(jpa.getCreatedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping grade level entity to model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting entity to model");
        }

    }

    public static GradeLevelJpaEntity toJpa(GradeLevel level) {
        try {
            return GradeLevelJpaEntity.builder()
                .id(level.getId())
                .name(level.getName())
                .description(level.getDescription())
                .order(PositiveInteger.valueOf(level.getOrder()))
                .status(GradeLevelStatus.value(level.getStatus()))
                .createdAt(level.getCreatedAt())
                .updatedAt(level.getUpdatedAt())
                .createdBy(level.getCreatedBy())
                .updatedBy(level.getUpdatedBy())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping grade level model to entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting model to entity");
        }

    }
}

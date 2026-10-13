package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.framework.FrameworkStatus;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.infrastructure.persistence.entity.FrameworkJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class FrameworkMapper {

    private FrameworkMapper() {}

    public static Framework toDomain(FrameworkJpaEntity jpa) {
        try {
            return Framework.builder()
                .id(jpa.getId())
                .code(Code.from(jpa.getCode()))
                .name(jpa.getName())
                .description(jpa.getDescription())
                .status(FrameworkStatus.from(jpa.getStatus()))
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .createdBy(jpa.getCreatedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping framework entity to model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting entity to model");
        }
    }

    public static FrameworkJpaEntity toJpa(Framework framework) {
        try {
            return FrameworkJpaEntity.builder()
                .id(framework.getId())
                .code(Code.valueOf(framework.getCode()))
                .name(framework.getName())
                .description(framework.getDescription())
                .status(FrameworkStatus.value(framework.getStatus()))
                .createdAt(framework.getCreatedAt())
                .updatedAt(framework.getUpdatedAt())
                .createdBy(framework.getCreatedBy())
                .updatedBy(framework.getUpdatedBy())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping framework model to entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting model to entity");
        }
    }
}

package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.valueobject.FrameworkCode;
import com.sep.vox.infrastructure.persistence.entity.framework.FrameworkJpaEntity;

public final class FrameworkMapper {

    private FrameworkMapper() {}

    public static Framework toDomain(FrameworkJpaEntity jpa) {
        return Framework.builder()
            .id(jpa.getId())
            .code(new FrameworkCode(jpa.getCode()))
            .name(jpa.getName())
            .description(jpa.getDescription())
            .active(jpa.isActive())
            .createdAt(jpa.getCreatedAt())
            .updatedAt(jpa.getUpdatedAt())
            .createdBy(jpa.getCreatedBy())
            .updatedBy(jpa.getUpdatedBy())
            .build();
    }

    public static FrameworkJpaEntity toJpa(Framework framework) {
        return FrameworkJpaEntity.builder()
            .id(framework.getId())
            .code(valueOf(framework.getCode()))
            .name(framework.getName())
            .description(framework.getDescription())
            .isActive(framework.isActive())
            .createdAt(framework.getCreatedAt())
            .updatedAt(framework.getUpdatedAt())
            .createdBy(framework.getCreatedBy())
            .updatedBy(framework.getUpdatedBy())
            .build();
    }

    private static String valueOf(FrameworkCode code) {
        return code == null ? null : code.value();
    }
}

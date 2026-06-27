package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.valueobject.FrameworkCode;
import com.sep.vox.infrastructure.persistence.entity.FrameworkJpaEntity;

public final class FrameworkMapper {

    private FrameworkMapper() {}

    public static Framework toDomain(FrameworkJpaEntity jpa) {
        return new Framework(
            jpa.getId(),
            new FrameworkCode(jpa.getCode()),
            jpa.getName(),
            jpa.getDescription(),
            jpa.isActive(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static FrameworkJpaEntity toJpa(Framework framework) {
        return new FrameworkJpaEntity(
            framework.getId(),
            valueOf(framework.getCode()),
            framework.getName(),
            framework.getDescription(),
            framework.isActive(),
            framework.getCreatedAt(),
            framework.getUpdatedAt(),
            framework.getCreatedBy(),
            framework.getUpdatedBy()
        );
    }

    private static String valueOf(FrameworkCode code) {
        return code == null ? null : code.value();
    }
}

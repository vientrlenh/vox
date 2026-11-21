package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.infrastructure.persistence.entity.framework.FrameworkVersionJpaEntity;

public final class FrameworkVersionMapper {

    private FrameworkVersionMapper() {}

    public static FrameworkVersion toDomain(FrameworkVersionJpaEntity jpa) {
        return FrameworkVersion.builder()
            .id(jpa.getId())
            .frameworkId(jpa.getFrameworkId())
            .code(jpa.getCode())
            .name(jpa.getName())
            .description(jpa.getDescription())
            .version(jpa.getVersion())
            .effectiveFrom(jpa.getEffectiveFrom())
            .effectiveTo(jpa.getEffectiveTo())
            .status(fromStatus(jpa.getStatus()))
            .createdAt(jpa.getCreatedAt())
            .updatedAt(jpa.getUpdatedAt())
            .createdBy(jpa.getCreatedBy())
            .updatedBy(jpa.getUpdatedBy())
            .build();
    }

    public static FrameworkVersionJpaEntity toJpa(FrameworkVersion version) {
        return FrameworkVersionJpaEntity.builder()
            .id(version.getId())
            .frameworkId(version.getFrameworkId())
            .code(version.getCode())
            .name(version.getName())
            .description(version.getDescription())
            .version(version.getVersion())
            .effectiveFrom(version.getEffectiveFrom())
            .effectiveTo(version.getEffectiveTo())
            .status(valueOf(version.getStatus()))
            .createdAt(version.getCreatedAt())
            .updatedAt(version.getUpdatedAt())
            .createdBy(version.getCreatedBy())
            .updatedBy(version.getUpdatedBy())
            .build();
    }

    private static String valueOf(FrameworkVersionStatus status) {
        return status == null ? null : status.name();
    }

    private static FrameworkVersionStatus fromStatus(String value) {
        return value == null ? null : FrameworkVersionStatus.valueOf(value);
    }
}

package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.infrastructure.persistence.entity.FrameworkVersionJpaEntity;

public final class FrameworkVersionMapper {

    private FrameworkVersionMapper() {}

    public static FrameworkVersion toDomain(FrameworkVersionJpaEntity jpa) {
        return new FrameworkVersion(
            jpa.getId(),
            jpa.getFrameworkId(),
            jpa.getCode(),
            jpa.getName(),
            jpa.getDescription(),
            jpa.getVersion(),
            jpa.getEffectiveFrom(),
            jpa.getEffectiveTo(),
            fromStatus(jpa.getStatus()),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static FrameworkVersionJpaEntity toJpa(FrameworkVersion version) {
        return new FrameworkVersionJpaEntity(
            version.getId(),
            version.getFrameworkId(),
            version.getCode(),
            version.getName(),
            version.getDescription(),
            version.getVersion(),
            version.getEffectiveFrom(),
            version.getEffectiveTo(),
            valueOf(version.getStatus()),
            version.getCreatedAt(),
            version.getUpdatedAt(),
            version.getCreatedBy(),
            version.getUpdatedBy()
        );
    }

    private static String valueOf(FrameworkVersionStatus status) {
        return status == null ? null : status.name();
    }

    private static FrameworkVersionStatus fromStatus(String value) {
        return value == null ? null : FrameworkVersionStatus.valueOf(value);
    }
}

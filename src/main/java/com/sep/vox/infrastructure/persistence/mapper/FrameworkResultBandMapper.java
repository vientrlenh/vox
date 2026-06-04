package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkResultBandStatus;
import com.sep.vox.infrastructure.persistence.entity.FrameworkResultBandJpaEntity;

public final class FrameworkResultBandMapper {

    private FrameworkResultBandMapper() {}

    public static FrameworkResultBand toDomain(FrameworkResultBandJpaEntity jpa) {
        return new FrameworkResultBand(
            jpa.getId(),
            jpa.getFrameworkVersionId(),
            jpa.getCode(),
            jpa.getLabel(),
            jpa.getDescription(),
            jpa.getScoreMin(),
            jpa.getScoreMax(),
            jpa.getOrder(), 
            statusFromString(jpa.getStatus()),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static FrameworkResultBandJpaEntity toJpa(FrameworkResultBand band) {
        return new FrameworkResultBandJpaEntity(
            band.getId(),
            band.getFrameworkVersionId(),
            band.getCode(),
            band.getLabel(),
            band.getDescription(),
            band.getScoreMin(),
            band.getScoreMax(),
            band.getOrder(),
            valueOf(band.getStatus()),
            band.getCreatedAt(),
            band.getUpdatedAt(),
            band.getCreatedBy(),
            band.getUpdatedBy()
        );
    }

    private static FrameworkResultBandStatus statusFromString(String status) {
        return status == null ? null : FrameworkResultBandStatus.valueOf(status);
    }

    private static String valueOf(FrameworkResultBandStatus status) {
        return status == null ? null : status.name();
    }
}

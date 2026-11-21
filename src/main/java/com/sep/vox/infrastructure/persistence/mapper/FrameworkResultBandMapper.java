package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.infrastructure.persistence.entity.framework.FrameworkResultBandJpaEntity;

public final class FrameworkResultBandMapper {

    private FrameworkResultBandMapper() {}

    public static FrameworkResultBand toDomain(FrameworkResultBandJpaEntity jpa) {
        return FrameworkResultBand.builder()
            .id(jpa.getId())
            .frameworkVersionId(jpa.getFrameworkVersionId())
            .code(jpa.getCode())
            .label(jpa.getLabel())
            .description(jpa.getDescription())
            .order(jpa.getOrder())
            .createdAt(jpa.getCreatedAt())
            .updatedAt(jpa.getUpdatedAt())
            .createdBy(jpa.getCreatedBy())
            .updatedBy(jpa.getUpdatedBy())
            .build();
    }

    public static FrameworkResultBandJpaEntity toJpa(FrameworkResultBand band) {
        return FrameworkResultBandJpaEntity.builder()
            .id(band.getId())
            .frameworkVersionId(band.getFrameworkVersionId())
            .code(band.getCode())
            .label(band.getLabel())
            .description(band.getDescription())
            .order(band.getOrder())
            .createdAt(band.getCreatedAt())
            .updatedAt(band.getUpdatedAt())
            .createdBy(band.getCreatedBy())
            .updatedBy(band.getUpdatedBy())
            .build();
    }

}

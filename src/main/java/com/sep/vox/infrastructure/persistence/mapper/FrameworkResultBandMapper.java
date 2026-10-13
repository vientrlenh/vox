package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.PositiveInteger;
import com.sep.vox.infrastructure.persistence.entity.FrameworkResultBandJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class FrameworkResultBandMapper {

    private FrameworkResultBandMapper() {}

    public static FrameworkResultBand toDomain(FrameworkResultBandJpaEntity jpa) {
        try {
            return FrameworkResultBand.builder()
                .id(jpa.getId())
                .frameworkVersionId(jpa.getFrameworkVersionId())
                .code(Code.from(jpa.getCode()))
                .label(jpa.getLabel())
                .description(jpa.getDescription())
                .order(PositiveInteger.from(jpa.getOrder()))
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping framework result band entity to model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting entity to model");
        }
    }

    public static FrameworkResultBandJpaEntity toJpa(FrameworkResultBand band) {
        try {
            return FrameworkResultBandJpaEntity.builder()
                .id(band.getId())
                .frameworkVersionId(band.getFrameworkVersionId())
                .code(Code.valueOf(band.getCode()))
                .label(band.getLabel())
                .description(band.getDescription())
                .order(PositiveInteger.valueOf(band.getOrder()))
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping framework result band model to entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting model to entity");
        }
    }
}

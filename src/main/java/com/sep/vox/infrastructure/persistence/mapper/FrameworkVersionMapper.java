package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.PositiveLong;
import com.sep.vox.infrastructure.persistence.entity.FrameworkVersionJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class FrameworkVersionMapper {

    private FrameworkVersionMapper() {}

    public static FrameworkVersion toDomain(FrameworkVersionJpaEntity jpa) {
        try {
            return FrameworkVersion.builder()
                .id(jpa.getId())
                .frameworkId(jpa.getFrameworkId())
                .code(Code.from(jpa.getCode()))
                .name(jpa.getName())
                .description(jpa.getDescription())
                .version(PositiveLong.from(jpa.getVersion()))
                .effectiveFrom(jpa.getEffectiveFrom())
                .effectiveTo(jpa.getEffectiveTo())
                .status(FrameworkVersionStatus.from(jpa.getStatus()))
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .createdBy(jpa.getCreatedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping framework version entity to model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting entity to model");
        }
    }

    public static FrameworkVersionJpaEntity toJpa(FrameworkVersion version) {
        try {
            return FrameworkVersionJpaEntity.builder()
                .id(version.getId())
                .frameworkId(version.getFrameworkId())
                .code(Code.valueOf(version.getCode()))
                .name(version.getName())
                .description(version.getDescription())
                .version(PositiveLong.valueOf(version.getVersion()))
                .effectiveFrom(version.getEffectiveFrom())
                .effectiveTo(version.getEffectiveTo())
                .status(FrameworkVersionStatus.value(version.getStatus()))
                .createdAt(version.getCreatedAt())
                .updatedAt(version.getUpdatedAt())
                .createdBy(version.getCreatedBy())
                .updatedBy(version.getUpdatedBy())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping framework version model to entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting model to entity");
        }
    }
}

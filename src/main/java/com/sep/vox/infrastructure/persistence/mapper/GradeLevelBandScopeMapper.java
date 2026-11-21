package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.gradelevel.GradeLevelBandScope;
import com.sep.vox.infrastructure.persistence.entity.gradelevel.GradeLevelBandScopeJpaEntity;

public final class GradeLevelBandScopeMapper {

    private GradeLevelBandScopeMapper() {}

    public static GradeLevelBandScope toDomain(GradeLevelBandScopeJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return GradeLevelBandScope.builder()
            .id(entity.getId())
            .gradeLevelId(entity.getGradeLevelId())
            .frameworkVersionId(entity.getFrameworkVersionId())
            .defaultTargetBandId(entity.getDefaultTargetBandId())
            .hardMaxBandId(entity.getHardMaxBandId())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }

    public static GradeLevelBandScopeJpaEntity toEntity(GradeLevelBandScope domain) {
        if (domain == null) {
            return null;
        }
        return GradeLevelBandScopeJpaEntity.builder()
            .id(domain.getId())
            .gradeLevelId(domain.getGradeLevelId())
            .frameworkVersionId(domain.getFrameworkVersionId())
            .defaultTargetBandId(domain.getDefaultTargetBandId())
            .hardMaxBandId(domain.getHardMaxBandId())
            .createdAt(domain.getCreatedAt())
            .updatedAt(domain.getUpdatedAt())
            .createdBy(domain.getCreatedBy())
            .updatedBy(domain.getUpdatedBy())
            .build();
    }
}

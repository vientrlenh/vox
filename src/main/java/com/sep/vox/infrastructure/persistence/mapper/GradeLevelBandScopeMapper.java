package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.gradelevel.GradeLevelBandScope;
import com.sep.vox.infrastructure.persistence.entity.GradeLevelBandScopeJpaEntity;

public final class GradeLevelBandScopeMapper {

    private GradeLevelBandScopeMapper() {}

    public static GradeLevelBandScope toDomain(GradeLevelBandScopeJpaEntity jpa) {
        return GradeLevelBandScope.builder()
            .id(jpa.getId())
            .gradeLevelId(jpa.getGradeLevelId())
            .frameworkVersionId(jpa.getFrameworkVersionId())
            .defaultTargetBandId(jpa.getDefaultTargetBandId())
            .hardMaxBandId(jpa.getHardMaxBandId())
            .createdAt(jpa.getCreatedAt())
            .updatedAt(jpa.getUpdatedAt())
            .createdBy(jpa.getCreatedBy())
            .updatedBy(jpa.getUpdatedBy())
            .build();
    }

    public static GradeLevelBandScopeJpaEntity toJpa(GradeLevelBandScope scope) {
        return GradeLevelBandScopeJpaEntity.builder()
            .id(scope.getId())
            .gradeLevelId(scope.getGradeLevelId())
            .frameworkVersionId(scope.getFrameworkVersionId())
            .defaultTargetBandId(scope.getDefaultTargetBandId())
            .hardMaxBandId(scope.getHardMaxBandId())
            .createdAt(scope.getCreatedAt())
            .updatedAt(scope.getUpdatedAt())
            .createdBy(scope.getCreatedBy())
            .updatedBy(scope.getUpdatedBy())
            .build();
    }
}

package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.gradelevel.GradeLevelBandScope;
import com.sep.vox.infrastructure.persistence.entity.GradeLevelBandScopeJpaEntity;

public final class GradeLevelBandScopeMapper {

    private GradeLevelBandScopeMapper() {}

    public static GradeLevelBandScope toDomain(GradeLevelBandScopeJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new GradeLevelBandScope(
                entity.getId(),
                entity.getGradeLevelId(),
                entity.getFrameworkVersionId(),
                entity.getDefaultTargetBandId(),
                entity.getHardMaxBandId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedBy());
    }

    public static GradeLevelBandScopeJpaEntity toEntity(GradeLevelBandScope domain) {
        if (domain == null) {
            return null;
        }
        return new GradeLevelBandScopeJpaEntity(
                domain.getId(),
                domain.getGradeLevelId(),
                domain.getFrameworkVersionId(),
                domain.getDefaultTargetBandId(),
                domain.getHardMaxBandId(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                domain.getCreatedBy(),
                domain.getUpdatedBy());
    }
}

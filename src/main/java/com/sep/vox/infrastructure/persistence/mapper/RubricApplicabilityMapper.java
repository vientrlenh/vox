package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.rubric.RubricApplicability;
import com.sep.vox.infrastructure.persistence.entity.RubricApplicabilityJpaEntity;

public class RubricApplicabilityMapper {

    private RubricApplicabilityMapper() {
        // Private constructor to prevent instantiation
    }

    // Chuyển từ Domain Model xuống JPA Entity để lưu vào DB
    public static RubricApplicabilityJpaEntity toJpa(RubricApplicability domain) {
        if (domain == null) {
            return null;
        }

        return new RubricApplicabilityJpaEntity(
                domain.getId(),
                domain.getRubricVersionId(),
                domain.getSchoolClassId(),
                domain.getSchoolGradeId(),
                domain.getEffectiveFrom(),
                domain.getEffectiveTo(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                domain.getCreatedBy(),
                domain.getUpdatedBy()
        );
    }

    // Chuyển từ JPA Entity lên Domain Model để UseCase xử lý
    public static RubricApplicability toDomain(RubricApplicabilityJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        RubricApplicability domain = new RubricApplicability(
                entity.getRubricVersionId(),
                entity.getSchoolClassId(),
                entity.getSchoolGradeId(),
                entity.getEffectiveFrom(),
                entity.getEffectiveTo(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedBy()
        );
        // Nhớ set ID vì khi bốc từ DB lên nó đã có ID rồi
        domain.setId(entity.getId());

        return domain;
    }
}
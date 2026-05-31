package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.infrastructure.persistence.entity.RubricVersionJpaEntity;

public final class RubricVersionMapper {

    private RubricVersionMapper() {}

    public static RubricVersion toDomain(RubricVersionJpaEntity jpa) {
        return new RubricVersion(
            jpa.getId(),
            jpa.getRubricId(),
            jpa.getVersion(),
            fromStatus(jpa.getStatus()),
            jpa.getEffectiveFrom(),
            jpa.getEffectiveTo(),
            jpa.getScoringScaleMin(),
            jpa.getScoringScaleMax(),
            fromTotalScoreMethod(jpa.getTotalScoreMethod()),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static RubricVersionJpaEntity toJpa(RubricVersion version) {
        return new RubricVersionJpaEntity(
            version.getId(),
            version.getRubricId(),
            version.getVersion(),
            valueOf(version.getStatus()),
            version.getEffectiveFrom(),
            version.getEffectiveTo(),
            version.getScoringScaleMin(),
            version.getScoringScaleMax(),
            valueOf(version.getTotalScoreMethod()),
            version.getCreatedAt(),
            version.getUpdatedAt(),
            version.getCreatedBy(),
            version.getUpdatedBy()
        );
    }

    private static String valueOf(RubricTotalScoreMethod method) {
        return method == null ? null : method.name();
    }

    private static String valueOf(RubricStatus status) {
        return status == null ? null : status.name();
    }

    private static RubricStatus fromStatus(String value) {
        return value == null ? null : RubricStatus.valueOf(value);
    }

    private static RubricTotalScoreMethod fromTotalScoreMethod(String value) {
        return value == null ? null : RubricTotalScoreMethod.valueOf(value);
    }
}

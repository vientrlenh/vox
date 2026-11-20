package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.infrastructure.persistence.entity.RubricVersionJpaEntity;

public final class RubricVersionMapper {

    private RubricVersionMapper() {}

    // sourceRubricVersionId đi qua setter ở cả hai chiều: nó cố ý không nằm trong constructor để
    // không bắt mọi nơi dựng RubricVersion phải thêm một tham số hầu hết đều là null.
    public static RubricVersion toDomain(RubricVersionJpaEntity jpa) {
        var domain = newDomain(jpa);
        domain.setSourceRubricVersionId(jpa.getSourceRubricVersionId());
        return domain;
    }

    private static RubricVersion newDomain(RubricVersionJpaEntity jpa) {
        return new RubricVersion(
            jpa.getId(),
            jpa.getRubricId(),
            jpa.getVersion(),
            jpa.getCode(), 
            jpa.getName(), 
            jpa.getDescription(), 
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
        var jpa = newJpa(version);
        jpa.setSourceRubricVersionId(version.getSourceRubricVersionId());
        return jpa;
    }

    private static RubricVersionJpaEntity newJpa(RubricVersion version) {
        return new RubricVersionJpaEntity(
            version.getId(),
            version.getRubricId(),
            version.getVersion(),
            version.getCode(),
            version.getName(), 
            version.getDescription(),
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

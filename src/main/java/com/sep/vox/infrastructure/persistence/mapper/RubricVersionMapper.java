package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.infrastructure.persistence.entity.rubric.RubricVersionJpaEntity;

public final class RubricVersionMapper {

    private RubricVersionMapper() {}

    // sourceRubricVersionId đi qua setter ở cả hai chiều: nó cố ý không nằm trong constructor để
    // không bắt mọi nơi dựng RubricVersion phải thêm một tham số hầu hết đều là null.
    public static RubricVersion toDomain(RubricVersionJpaEntity jpa) {
        RubricVersion domain = newDomain(jpa);
        domain.setSourceRubricVersionId(jpa.getSourceRubricVersionId());
        return domain;
    }

    private static RubricVersion newDomain(RubricVersionJpaEntity jpa) {
        return RubricVersion.builder()
                .id(jpa.getId())
                .rubricId(jpa.getRubricId())
                .version(jpa.getVersion())
                .code(jpa.getCode())
                .name(jpa.getName())
                .description(jpa.getDescription())
                .status(fromStatus(jpa.getStatus()))
                .effectiveFrom(jpa.getEffectiveFrom())
                .effectiveTo(jpa.getEffectiveTo())
                .scoringScaleMin(jpa.getScoringScaleMin())
                .scoringScaleMax(jpa.getScoringScaleMax())
                .totalScoreMethod(fromTotalScoreMethod(jpa.getTotalScoreMethod()))
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .createdBy(jpa.getCreatedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
    }

    public static RubricVersionJpaEntity toJpa(RubricVersion version) {
        RubricVersionJpaEntity jpa = newJpa(version);
        jpa.setSourceRubricVersionId(version.getSourceRubricVersionId());
        return jpa;
    }

    private static RubricVersionJpaEntity newJpa(RubricVersion version) {
        return RubricVersionJpaEntity.builder()
                .id(version.getId())
                .rubricId(version.getRubricId())
                .version(version.getVersion())
                .code(version.getCode())
                .name(version.getName())
                .description(version.getDescription())
                .status(valueOf(version.getStatus()))
                .effectiveFrom(version.getEffectiveFrom())
                .effectiveTo(version.getEffectiveTo())
                .scoringScaleMin(version.getScoringScaleMin())
                .scoringScaleMax(version.getScoringScaleMax())
                .totalScoreMethod(valueOf(version.getTotalScoreMethod()))
                .createdAt(version.getCreatedAt())
                .updatedAt(version.getUpdatedAt())
                .createdBy(version.getCreatedBy())
                .updatedBy(version.getUpdatedBy())
                .build();
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

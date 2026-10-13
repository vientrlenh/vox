package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.valueobject.PositiveDecimal;
import com.sep.vox.domain.valueobject.PositiveInteger;
import com.sep.vox.infrastructure.persistence.entity.RubricVersionJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class RubricVersionMapper {

    private RubricVersionMapper() {}

    public static RubricVersion toDomain(RubricVersionJpaEntity jpa) {
        try {
            return RubricVersion.builder()
                .id(jpa.getId())
                .rubricId(jpa.getRubricId())
                .version(PositiveInteger.from(jpa.getVersion()))
                .name(jpa.getName())
                .description(jpa.getDescription())
                .status(RubricStatus.from(jpa.getStatus()))
                .effectiveFrom(jpa.getEffectiveFrom())
                .effectiveTo(jpa.getEffectiveTo())
                .scoringScaleMin(PositiveDecimal.from(jpa.getScoringScaleMin()))
                .scoringScaleMax(PositiveDecimal.from(jpa.getScoringScaleMax()))
                .totalScoreMethod(RubricTotalScoreMethod.from(jpa.getTotalScoreMethod()))
                .sourceRubricVersionId(jpa.getSourceRubricVersionId())
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .createdBy(jpa.getCreatedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping rubric version entity to model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting entity to model");
        }
    }

    public static RubricVersionJpaEntity toJpa(RubricVersion version) {
        try {
            return RubricVersionJpaEntity.builder()
                .id(version.getId())
                .rubricId(version.getRubricId())
                .version(PositiveInteger.valueOf(version.getVersion()))
                .name(version.getName())
                .description(version.getDescription())
                .status(RubricStatus.value(version.getStatus()))
                .effectiveFrom(version.getEffectiveFrom())
                .effectiveTo(version.getEffectiveTo())
                .scoringScaleMin(PositiveDecimal.valueOf(version.getScoringScaleMin()))
                .scoringScaleMax(PositiveDecimal.valueOf(version.getScoringScaleMax()))
                .totalScoreMethod(RubricTotalScoreMethod.value(version.getTotalScoreMethod()))
                .sourceRubricVersionId(version.getSourceRubricVersionId())
                .createdAt(version.getCreatedAt())
                .updatedAt(version.getUpdatedAt())
                .createdBy(version.getCreatedBy())
                .updatedBy(version.getUpdatedBy())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping rubric version model to entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting model to entity");
        }
    }
}

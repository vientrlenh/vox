package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentBlueprintVersionJpaEntity;

public final class AssessmentBlueprintVersionMapper {

    private AssessmentBlueprintVersionMapper() {
    }

    public static AssessmentBlueprintVersion toDomain(AssessmentBlueprintVersionJpaEntity jpa) {
        return AssessmentBlueprintVersion.builder()
                .id(jpa.getId())
                .blueprintId(jpa.getBlueprintId())
                .version(jpa.getVersion())
                .code(jpa.getCode())
                .description(jpa.getDescription())
                .status(statusFromString(jpa.getStatus()))
                .totalTimeLimitSeconds(jpa.getTotalTimeLimitSeconds())
                .effectiveFrom(jpa.getEffectiveFrom())
                .effectiveTo(jpa.getEffectiveTo())
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .createdBy(jpa.getCreatedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
    }

    public static AssessmentBlueprintVersionJpaEntity toJpa(AssessmentBlueprintVersion domain) {
        return AssessmentBlueprintVersionJpaEntity.builder()
                .id(domain.getId())
                .blueprintId(domain.getBlueprintId())
                .version(domain.getVersion())
                .code(domain.getCode())
                .description(domain.getDescription())
                .status(domain.getStatus().name())
                .totalTimeLimitSeconds(domain.getTotalTimeLimitSeconds())
                .effectiveFrom(domain.getEffectiveFrom())
                .effectiveTo(domain.getEffectiveTo())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .createdBy(domain.getCreatedBy())
                .updatedBy(domain.getUpdatedBy())
                .build();
    }

    private static AssessmentBlueprintVersionStatus statusFromString(String value) {
        return value == null ? null : AssessmentBlueprintVersionStatus.valueOf(value);
    }
}

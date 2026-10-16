package com.sep.vox.infrastructure.persistence.mapper.assessment;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentBlueprintVersionJpaEntity;

public final class AssessmentBlueprintVersionMapper {

    private AssessmentBlueprintVersionMapper() {
    }

    public static AssessmentBlueprintVersion toDomain(AssessmentBlueprintVersionJpaEntity jpa) {
        return new AssessmentBlueprintVersion(
            jpa.getId(),
            jpa.getBlueprintId(),
            jpa.getVersion(),
            jpa.getCode(),
            jpa.getDescription(),
            statusFromString(jpa.getStatus()),
            jpa.getTotalTimeLimitSeconds(),
            jpa.getEffectiveFrom(),
            jpa.getEffectiveTo(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static AssessmentBlueprintVersionJpaEntity toJpa(AssessmentBlueprintVersion domain) {
        return new AssessmentBlueprintVersionJpaEntity(
            domain.getId(),
            domain.getBlueprintId(),
            domain.getVersion(),
            domain.getCode(),
            domain.getDescription(),
            domain.getStatus().name(),
            domain.getTotalTimeLimitSeconds(),
            domain.getEffectiveFrom(),
            domain.getEffectiveTo(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
        );
    }

    private static AssessmentBlueprintVersionStatus statusFromString(String value) {
        return value == null ? null : AssessmentBlueprintVersionStatus.valueOf(value);
    }
}

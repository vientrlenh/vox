package com.sep.vox.infrastructure.persistence.mapper.assessment;

import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentBlueprintJpaEntity;

public final class AssessmentBlueprintMapper {

    private AssessmentBlueprintMapper() {
    }

    public static AssessmentBlueprint toDomain(AssessmentBlueprintJpaEntity jpa) {
        return new AssessmentBlueprint(
            jpa.getId(),
            jpa.getSchoolId(),
            jpa.getLanguageId(),
            jpa.getGradeLevelId(),
            jpa.getCode(),
            jpa.getName(),
            jpa.getDescription(),
            jpa.isActive(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static AssessmentBlueprintJpaEntity toJpa(AssessmentBlueprint domain) {
        return new AssessmentBlueprintJpaEntity(
            domain.getId(),
            domain.getSchoolId(),
            domain.getLanguageId(),
            domain.getGradeLevelId(),
            domain.getCode(),
            domain.getName(),
            domain.getDescription(),
            domain.isActive(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
        );
    }
}

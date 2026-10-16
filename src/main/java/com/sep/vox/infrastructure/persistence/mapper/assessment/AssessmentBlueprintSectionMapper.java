package com.sep.vox.infrastructure.persistence.mapper.assessment;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentBlueprintSectionJpaEntity;

public final class AssessmentBlueprintSectionMapper {

    private AssessmentBlueprintSectionMapper() {
    }

    public static AssessmentBlueprintSection toDomain(AssessmentBlueprintSectionJpaEntity jpa) {
        return new AssessmentBlueprintSection(
            jpa.getId(),
            jpa.getBlueprintVersionId(),
            jpa.getOrder(),
            jpa.getTitle(),
            jpa.getInstruction(),
            jpa.getSectionTimeLimitSeconds(),
            jpa.getSectionWeight(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static AssessmentBlueprintSectionJpaEntity toJpa(AssessmentBlueprintSection domain) {
        return new AssessmentBlueprintSectionJpaEntity(
            domain.getId(),
            domain.getBlueprintVersionId(),
            domain.getOrder(),
            domain.getTitle(),
            domain.getInstruction(),
            domain.getSectionTimeLimitSeconds(),
            domain.getSectionWeight(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
        );
    }
}

package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentBlueprintSectionJpaEntity;

public final class AssessmentBlueprintSectionMapper {

    private AssessmentBlueprintSectionMapper() {
    }

    public static AssessmentBlueprintSection toDomain(AssessmentBlueprintSectionJpaEntity jpa) {
        return AssessmentBlueprintSection.builder()
                .id(jpa.getId())
                .blueprintVersionId(jpa.getBlueprintVersionId())
                .order(jpa.getOrder())
                .title(jpa.getTitle())
                .instruction(jpa.getInstruction())
                .sectionTimeLimitSeconds(jpa.getSectionTimeLimitSeconds())
                .sectionWeight(jpa.getSectionWeight())
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .createdBy(jpa.getCreatedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
    }

    public static AssessmentBlueprintSectionJpaEntity toJpa(AssessmentBlueprintSection domain) {
        return AssessmentBlueprintSectionJpaEntity.builder()
                .id(domain.getId())
                .blueprintVersionId(domain.getBlueprintVersionId())
                .order(domain.getOrder())
                .title(domain.getTitle())
                .instruction(domain.getInstruction())
                .sectionTimeLimitSeconds(domain.getSectionTimeLimitSeconds())
                .sectionWeight(domain.getSectionWeight())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .createdBy(domain.getCreatedBy())
                .updatedBy(domain.getUpdatedBy())
                .build();
    }
}

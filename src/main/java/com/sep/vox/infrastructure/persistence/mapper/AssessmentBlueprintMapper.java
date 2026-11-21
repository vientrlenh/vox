package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentBlueprintJpaEntity;

public final class AssessmentBlueprintMapper {

    private AssessmentBlueprintMapper() {
    }

    public static AssessmentBlueprint toDomain(AssessmentBlueprintJpaEntity jpa) {
        return AssessmentBlueprint.builder()
                .id(jpa.getId())
                .schoolId(jpa.getSchoolId())
                .languageId(jpa.getLanguageId())
                .gradeLevelId(jpa.getGradeLevelId())
                .code(jpa.getCode())
                .name(jpa.getName())
                .description(jpa.getDescription())
                .active(jpa.isActive())
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .createdBy(jpa.getCreatedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
    }

    public static AssessmentBlueprintJpaEntity toJpa(AssessmentBlueprint domain) {
        return AssessmentBlueprintJpaEntity.builder()
                .id(domain.getId())
                .schoolId(domain.getSchoolId())
                .languageId(domain.getLanguageId())
                .gradeLevelId(domain.getGradeLevelId())
                .code(domain.getCode())
                .name(domain.getName())
                .description(domain.getDescription())
                .isActive(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .createdBy(domain.getCreatedBy())
                .updatedBy(domain.getUpdatedBy())
                .build();
    }
}

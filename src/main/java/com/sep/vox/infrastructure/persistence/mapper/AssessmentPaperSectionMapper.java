package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.AssessmentPaperSection;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentPaperSectionJpaEntity;

public final class AssessmentPaperSectionMapper {

    private AssessmentPaperSectionMapper() {}

    public static AssessmentPaperSection toDomain(AssessmentPaperSectionJpaEntity jpa) {
        return AssessmentPaperSection.builder()
                .id(jpa.getId())
                .paperId(jpa.getPaperId())
                .order(jpa.getOrder())
                .title(jpa.getTitle())
                .instruction(jpa.getInstruction())
                .sectionTimeLimitSeconds(jpa.getSectionTimeLimitSeconds())
                .weight(jpa.getWeight())
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .createdBy(jpa.getCreatedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
    }

    public static AssessmentPaperSectionJpaEntity toJpa(AssessmentPaperSection domain) {
        return AssessmentPaperSectionJpaEntity.builder()
                .id(domain.getId())
                .paperId(domain.getPaperId())
                .order(domain.getOrder())
                .title(domain.getTitle())
                .instruction(domain.getInstruction())
                .sectionTimeLimitSeconds(domain.getSectionTimeLimitSeconds())
                .weight(domain.getWeight())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .createdBy(domain.getCreatedBy())
                .updatedBy(domain.getUpdatedBy())
                .build();
    }
}

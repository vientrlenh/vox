package com.sep.vox.infrastructure.persistence.mapper.assessment;

import com.sep.vox.domain.model.assessment.AssessmentPaperSection;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentPaperSectionJpaEntity;

public final class AssessmentPaperSectionMapper {

    private AssessmentPaperSectionMapper() {}

    public static AssessmentPaperSection toDomain(AssessmentPaperSectionJpaEntity jpa) {
        return new AssessmentPaperSection(
            jpa.getId(),
            jpa.getPaperId(),
            jpa.getOrder(),
            jpa.getTitle(),
            jpa.getInstruction(),
            jpa.getSectionTimeLimitSeconds(),
            jpa.getWeight(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static AssessmentPaperSectionJpaEntity toJpa(AssessmentPaperSection domain) {
        return new AssessmentPaperSectionJpaEntity(
            domain.getId(),
            domain.getPaperId(),
            domain.getOrder(),
            domain.getTitle(),
            domain.getInstruction(),
            domain.getSectionTimeLimitSeconds(),
            domain.getWeight(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
        );
    }
}

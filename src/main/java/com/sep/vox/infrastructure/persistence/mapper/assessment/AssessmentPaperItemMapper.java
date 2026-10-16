package com.sep.vox.infrastructure.persistence.mapper.assessment;

import com.sep.vox.domain.model.assessment.AssessmentPaperItem;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentPaperItemJpaEntity;

public final class AssessmentPaperItemMapper {

    private AssessmentPaperItemMapper() {}

    public static AssessmentPaperItem toDomain(AssessmentPaperItemJpaEntity jpa) {
        return new AssessmentPaperItem(
            jpa.getId(),
            jpa.getBlueprintSlotId(),
            jpa.getSectionId(),
            jpa.getPaperId(),
            jpa.getQuestionId(),
            jpa.getOrder(),
            jpa.getWeight()
        );
    }

    public static AssessmentPaperItemJpaEntity toJpa(AssessmentPaperItem domain) {
        return new AssessmentPaperItemJpaEntity(
            domain.getId(),
            domain.getBlueprintSlotId(),
            domain.getSectionId(),
            domain.getPaperId(),
            domain.getQuestionId(),
            domain.getOrder(),
            domain.getWeight()
        );
    }
}

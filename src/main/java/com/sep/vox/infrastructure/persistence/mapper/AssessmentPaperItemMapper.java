package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.AssessmentPaperItem;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentPaperItemJpaEntity;

public final class AssessmentPaperItemMapper {

    private AssessmentPaperItemMapper() {}

    public static AssessmentPaperItem toDomain(AssessmentPaperItemJpaEntity jpa) {
        return AssessmentPaperItem.builder()
                .id(jpa.getId())
                .blueprintSlotId(jpa.getBlueprintSlotId())
                .sectionId(jpa.getSectionId())
                .paperId(jpa.getPaperId())
                .questionId(jpa.getQuestionId())
                .order(jpa.getOrder())
                .weight(jpa.getWeight())
                .build();
    }

    public static AssessmentPaperItemJpaEntity toJpa(AssessmentPaperItem domain) {
        return AssessmentPaperItemJpaEntity.builder()
                .id(domain.getId())
                .blueprintSlotId(domain.getBlueprintSlotId())
                .sectionId(domain.getSectionId())
                .paperId(domain.getPaperId())
                .questionId(domain.getQuestionId())
                .order(domain.getOrder())
                .weight(domain.getWeight())
                .build();
    }
}

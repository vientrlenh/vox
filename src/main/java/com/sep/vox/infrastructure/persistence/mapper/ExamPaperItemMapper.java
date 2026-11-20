package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamPaperItem;
import com.sep.vox.infrastructure.persistence.entity.ExamPaperItemJpaEntity;

public final class ExamPaperItemMapper {

    private ExamPaperItemMapper() {}

    public static ExamPaperItem toDomain(ExamPaperItemJpaEntity jpa) {
        return new ExamPaperItem(
            jpa.getId(),
            jpa.getBlueprintSlotId(),
            jpa.getSectionId(),
            jpa.getPaperId(),
            jpa.getQuestionId(),
            jpa.getOrder(),
            jpa.getWeight()
        );
    }

    public static ExamPaperItemJpaEntity toJpa(ExamPaperItem domain) {
        return new ExamPaperItemJpaEntity(
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

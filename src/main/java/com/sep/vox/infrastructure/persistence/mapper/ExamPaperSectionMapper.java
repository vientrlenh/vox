package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamPaperSection;
import com.sep.vox.infrastructure.persistence.entity.ExamPaperSectionJpaEntity;

public final class ExamPaperSectionMapper {

    private ExamPaperSectionMapper() {}

    public static ExamPaperSection toDomain(ExamPaperSectionJpaEntity jpa) {
        return new ExamPaperSection(
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

    public static ExamPaperSectionJpaEntity toJpa(ExamPaperSection domain) {
        return new ExamPaperSectionJpaEntity(
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

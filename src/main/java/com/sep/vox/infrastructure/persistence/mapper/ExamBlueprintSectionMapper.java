package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamBlueprintSection;
import com.sep.vox.infrastructure.persistence.entity.ExamBlueprintSectionJpaEntity;

public final class ExamBlueprintSectionMapper {

    private ExamBlueprintSectionMapper() {
    }

    public static ExamBlueprintSection toDomain(ExamBlueprintSectionJpaEntity jpa) {
        return new ExamBlueprintSection(
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

    public static ExamBlueprintSectionJpaEntity toJpa(ExamBlueprintSection domain) {
        return new ExamBlueprintSectionJpaEntity(
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

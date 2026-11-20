package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamBlueprint;
import com.sep.vox.infrastructure.persistence.entity.ExamBlueprintJpaEntity;

public final class ExamBlueprintMapper {

    private ExamBlueprintMapper() {
    }

    public static ExamBlueprint toDomain(ExamBlueprintJpaEntity jpa) {
        return new ExamBlueprint(
            jpa.getId(),
            jpa.getSchoolId(),
            jpa.getLanguageId(),
            jpa.getGradeLevelId(),
            jpa.getCode(),
            jpa.getName(),
            jpa.getDescription(),
            jpa.isActive(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static ExamBlueprintJpaEntity toJpa(ExamBlueprint domain) {
        return new ExamBlueprintJpaEntity(
            domain.getId(),
            domain.getSchoolId(),
            domain.getLanguageId(),
            domain.getGradeLevelId(),
            domain.getCode(),
            domain.getName(),
            domain.getDescription(),
            domain.isActive(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
        );
    }
}

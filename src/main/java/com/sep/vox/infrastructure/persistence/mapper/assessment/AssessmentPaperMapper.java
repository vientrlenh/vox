package com.sep.vox.infrastructure.persistence.mapper.assessment;

import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentPaperJpaEntity;

public final class AssessmentPaperMapper {

    private AssessmentPaperMapper() {}

    public static AssessmentPaper toDomain(AssessmentPaperJpaEntity jpa) {
        return new AssessmentPaper(
            jpa.getId(),
            jpa.getAssessmentId(),
            jpa.getBlueprintVersionId(),
            jpa.getCode(),
            jpa.getVariant(),
            statusFromString(jpa.getStatus()),
            jpa.getTimeDurationSeconds(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static AssessmentPaperJpaEntity toJpa(AssessmentPaper domain) {
        return new AssessmentPaperJpaEntity(
            domain.getId(),
            domain.getAssessmentId(),
            domain.getBlueprintVersionId(),
            domain.getCode(),
            domain.getVariant(),
            domain.getStatus().name(),
            domain.getTimeDurationSeconds(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
        );
    }

    private static AssessmentPaperStatus statusFromString(String status) {
        return status == null ? null : AssessmentPaperStatus.valueOf(status);
    }
}

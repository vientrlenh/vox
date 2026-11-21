package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentPaperJpaEntity;

public final class AssessmentPaperMapper {

    private AssessmentPaperMapper() {}

    public static AssessmentPaper toDomain(AssessmentPaperJpaEntity jpa) {
        return AssessmentPaper.builder()
                .id(jpa.getId())
                .assessmentId(jpa.getAssessmentId())
                .blueprintVersionId(jpa.getBlueprintVersionId())
                .code(jpa.getCode())
                .variant(jpa.getVariant())
                .status(statusFromString(jpa.getStatus()))
                .timeDurationSeconds(jpa.getTimeDurationSeconds())
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .createdBy(jpa.getCreatedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
    }

    public static AssessmentPaperJpaEntity toJpa(AssessmentPaper domain) {
        return AssessmentPaperJpaEntity.builder()
                .id(domain.getId())
                .assessmentId(domain.getAssessmentId())
                .blueprintVersionId(domain.getBlueprintVersionId())
                .code(domain.getCode())
                .variant(domain.getVariant())
                .status(domain.getStatus().name())
                .timeDurationSeconds(domain.getTimeDurationSeconds())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .createdBy(domain.getCreatedBy())
                .updatedBy(domain.getUpdatedBy())
                .build();
    }

    private static AssessmentPaperStatus statusFromString(String status) {
        return status == null ? null : AssessmentPaperStatus.valueOf(status);
    }
}

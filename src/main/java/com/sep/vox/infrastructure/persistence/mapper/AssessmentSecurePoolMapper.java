package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.AssessmentSecurePool;
import com.sep.vox.domain.model.assessment.AssessmentSecurePoolReleaseMode;
import com.sep.vox.domain.model.assessment.AssessmentSecurePoolStatus;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentSecurePoolJpaEntity;

public final class AssessmentSecurePoolMapper {

    private AssessmentSecurePoolMapper() {}

    public static AssessmentSecurePool toDomain(AssessmentSecurePoolJpaEntity jpa) {
        return AssessmentSecurePool.builder()
                .id(jpa.getId())
                .assessmentId(jpa.getAssessmentId())
                .status(statusFromString(jpa.getStatus()))
                .releaseMode(releaseModeFromString(jpa.getReleaseMode()))
                .embargoUntil(jpa.getEmbargoUntil())
                .releasedAt(jpa.getReleasedAt())
                .releasedBy(jpa.getReleasedBy())
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .createdBy(jpa.getCreatedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
    }

    public static AssessmentSecurePoolJpaEntity toJpa(AssessmentSecurePool domain) {
        return AssessmentSecurePoolJpaEntity.builder()
                .id(domain.getId())
                .assessmentId(domain.getAssessmentId())
                .status(domain.getStatus().name())
                .releaseMode(domain.getReleaseMode().name())
                .embargoUntil(domain.getEmbargoUntil())
                .releasedAt(domain.getReleasedAt())
                .releasedBy(domain.getReleasedBy())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .createdBy(domain.getCreatedBy())
                .updatedBy(domain.getUpdatedBy())
                .build();
    }

    private static AssessmentSecurePoolStatus statusFromString(String status) {
        return status == null ? null : AssessmentSecurePoolStatus.valueOf(status);
    }

    private static AssessmentSecurePoolReleaseMode releaseModeFromString(String releaseMode) {
        return releaseMode == null ? null : AssessmentSecurePoolReleaseMode.valueOf(releaseMode);
    }
}

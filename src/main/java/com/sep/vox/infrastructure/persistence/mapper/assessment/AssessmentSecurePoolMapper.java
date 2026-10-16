package com.sep.vox.infrastructure.persistence.mapper.assessment;

import com.sep.vox.domain.model.assessment.AssessmentSecurePool;
import com.sep.vox.domain.model.assessment.AssessmentSecurePoolReleaseMode;
import com.sep.vox.domain.model.assessment.AssessmentSecurePoolStatus;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentSecurePoolJpaEntity;

public final class AssessmentSecurePoolMapper {

    private AssessmentSecurePoolMapper() {}

    public static AssessmentSecurePool toDomain(AssessmentSecurePoolJpaEntity jpa) {
        return new AssessmentSecurePool(
            jpa.getId(),
            jpa.getAssessmentId(),
            statusFromString(jpa.getStatus()),
            releaseModeFromString(jpa.getReleaseMode()),
            jpa.getEmbargoUntil(),
            jpa.getReleasedAt(),
            jpa.getReleasedBy(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static AssessmentSecurePoolJpaEntity toJpa(AssessmentSecurePool domain) {
        return new AssessmentSecurePoolJpaEntity(
            domain.getId(),
            domain.getAssessmentId(),
            domain.getStatus().name(),
            domain.getReleaseMode().name(),
            domain.getEmbargoUntil(),
            domain.getReleasedAt(),
            domain.getReleasedBy(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
        );
    }

    private static AssessmentSecurePoolStatus statusFromString(String status) {
        return status == null ? null : AssessmentSecurePoolStatus.valueOf(status);
    }

    private static AssessmentSecurePoolReleaseMode releaseModeFromString(String releaseMode) {
        return releaseMode == null ? null : AssessmentSecurePoolReleaseMode.valueOf(releaseMode);
    }
}

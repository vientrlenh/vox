package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentSecurePool;
import com.sep.vox.domain.model.assessment.AssessmentSecurePoolReleaseMode;
import com.sep.vox.domain.model.assessment.AssessmentSecurePoolStatus;

public record AssessmentSecurePoolDto(
    UUID id,
    UUID assessmentId,
    AssessmentSecurePoolStatus status,
    AssessmentSecurePoolReleaseMode releaseMode,
    Instant embargoUntil,
    Instant releasedAt,
    UUID releasedBy
) {

    public static AssessmentSecurePoolDto toDto(AssessmentSecurePool assessmentSecurePool) {
        return new AssessmentSecurePoolDto(
            assessmentSecurePool.getId(),
            assessmentSecurePool.getAssessmentId(),
            assessmentSecurePool.getStatus(),
            assessmentSecurePool.getReleaseMode(),
            assessmentSecurePool.getEmbargoUntil(),
            assessmentSecurePool.getReleasedAt(),
            assessmentSecurePool.getReleasedBy()
        );
    }
}

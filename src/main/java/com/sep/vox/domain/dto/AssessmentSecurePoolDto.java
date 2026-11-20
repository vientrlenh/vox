package com.sep.vox.domain.dto;

import com.sep.vox.domain.model.assessment.AssessmentSecurePoolStatus;
import com.sep.vox.domain.model.assessment.AssessmentSecurePoolReleaseMode;
import java.time.Instant;
import java.util.UUID;

public record AssessmentSecurePoolDto(
    UUID id,
    UUID assessmentId,
    AssessmentSecurePoolStatus status,
    AssessmentSecurePoolReleaseMode releaseMode,
    Instant embargoUntil,
    Instant releasedAt,
    UUID releasedBy
) {
}

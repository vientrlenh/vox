package com.sep.vox.domain.dto;

import java.util.UUID;

public record AssessmentSecurePoolDto(
    UUID id,
    UUID assessmentId,
    String status,
    String releaseMode,
    String embargoUntil,
    String releasedAt,
    UUID releasedBy
) {
}

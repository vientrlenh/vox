package com.sep.vox.domain.dto;

import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import java.time.Instant;
import java.util.UUID;

public record AssessmentPaperDto(
    UUID id,
    UUID assessmentId,
    UUID blueprintVersionId,
    String code,
    int variant,
    AssessmentPaperStatus status,
    Integer timeDurationSeconds,
    Instant createdAt,
    Instant updatedAt,
    UUID createdBy,
    UUID updatedBy
) {
}

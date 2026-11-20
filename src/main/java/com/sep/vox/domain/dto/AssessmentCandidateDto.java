package com.sep.vox.domain.dto;

import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import java.time.Instant;
import java.util.UUID;

public record AssessmentCandidateDto(
    UUID id,
    UUID assessmentId,
    UUID studentId,
    UUID assignedPaperId,
    UUID scheduleId,
    AssessmentCandidateStatus status,
    Instant assignedAt,
    Instant updatedAt,
    Instant blockedAt
) {
}

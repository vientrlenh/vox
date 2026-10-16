package com.sep.vox.domain.dto;

import java.util.UUID;

public record AssessmentCandidateDto(
    UUID id,
    UUID assessmentId,
    UUID studentId,
    UUID assignedPaperId,
    UUID scheduleId,
    String status,
    String assignedAt,
    String updatedAt,
    String blockedAt
) {
}

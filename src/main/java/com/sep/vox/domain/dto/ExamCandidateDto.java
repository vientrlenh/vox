package com.sep.vox.domain.dto;

import java.util.UUID;

public record ExamCandidateDto(
    UUID id,
    UUID examId,
    UUID studentId,
    UUID assignedPaperId,
    UUID scheduleId,
    String status,
    String assignedAt,
    String updatedAt,
    String blockedAt
) {
}

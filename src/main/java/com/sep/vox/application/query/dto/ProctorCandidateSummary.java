package com.sep.vox.application.query.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProctorCandidateSummary(
    UUID candidateId,
    UUID studentId,
    String studentName,
    String studentEmail,
    String status,
    OffsetDateTime blockedAt,
    UUID sessionId,
    String sessionStatus,
    boolean sessionFlagged
) {
}

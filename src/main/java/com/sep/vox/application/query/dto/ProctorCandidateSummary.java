package com.sep.vox.application.query.dto;

import java.time.Instant;
import java.util.UUID;

public record ProctorCandidateSummary(
    UUID candidateId,
    UUID studentId,
    String studentName,
    String studentEmail,
    String status,
    Instant blockedAt,
    UUID sessionId,
    String sessionStatus,
    boolean sessionFlagged
) {
}

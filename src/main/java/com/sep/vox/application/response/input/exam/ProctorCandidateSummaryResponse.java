package com.sep.vox.application.response.input.exam;

import java.util.UUID;

public record ProctorCandidateSummaryResponse(
    UUID candidateId,
    UUID studentId,
    String studentName,
    String studentEmail,
    String status,
    String blockedAt,
    UUID sessionId,
    String sessionStatus,
    boolean sessionFlagged
) {
}

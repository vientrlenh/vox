package com.sep.vox.application.response.input.assessmentsession;

import java.util.UUID;

public record AssessmentSessionResponse(
    UUID id,
    UUID assessmentId,
    UUID candidateId,
    UUID paperId,
    String startedAt,
    String submittedAt,
    String status,
    boolean flagged,
    String flagReason,
    boolean candidateBlocked
) {
}

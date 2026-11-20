package com.sep.vox.application.response.input.assessmentsession;

import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import java.time.Instant;
import java.util.UUID;

public record AssessmentSessionResponse(
    UUID id,
    UUID assessmentId,
    UUID candidateId,
    UUID paperId,
    Instant startedAt,
    Instant submittedAt,
    AssessmentSessionStatus status,
    boolean flagged,
    String flagReason,
    boolean candidateBlocked
) {
}

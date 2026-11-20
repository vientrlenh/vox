package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;

/**
 * One milestone in the score timeline of an attempt.
 *
 * <p>{@code actorName} is {@code null} when the system changed it itself (the AI finished grading, the
 * finalize job) — the FE shows "System" (the Vietnamese UI label "Hệ thống"), it does not leave it blank.
 */
public record ResultStatusHistoryInfo(
    UUID id,
    UUID candidateResultId,
    AssessmentCandidateResultStatus fromStatus,
    AssessmentCandidateResultStatus toStatus,
    BigDecimal scoreBefore,
    BigDecimal scoreAfter,
    String source,
    UUID actorId,
    String actorName,
    String reason,
    Instant createdAt
) {
}

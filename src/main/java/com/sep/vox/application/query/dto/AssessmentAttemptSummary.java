package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;

public record AssessmentAttemptSummary(
    UUID candidateId,
    UUID assessmentId,
    AssessmentCandidateStatus candidateStatus,
    UUID sessionId,
    Instant startedAt,
    Instant submittedAt,
    AssessmentSessionStatus status,
    boolean flagged,
    String flagReason,
    BigDecimal totalScore,
    /**
     * The score scale of the rubric that graded this attempt -- added 2026-08-11.
     *
     * <p>Without it the client has to guess, and has guessed wrong: both web and Flutter color the
     * score by hard thresholds 80/45, which is right for a 0-100 scale but turns EVERY score of a
     * 0-10 rubric red, even a 10/10 submission.
     *
     * <p>The attempt list mixes many assessments, each of which may use a rubric with a different
     * scale -- so the scale must travel with EACH attempt and cannot be derived from any shared config.
     */
    BigDecimal scoringScaleMin,
    BigDecimal scoringScaleMax,
    UUID rubricResultBandId,
    String rubricResultBandCode,
    String rubricResultBandName,
    AssessmentCandidateResultStatus resultStatus,
    /**
     * The reason the attempt was soft-deleted, null if not deleted. Goes with {@code status = DELETED}.
     *
     * <p>Only has a value on the read path of the school admin / council chair
     * ({@code findByCandidateIdsIncludingDeleted}); the student's path does not return deleted
     * attempts, so this field is always null there.
     */
    String deletedReason
) {
    // Auxiliary constructor for JPQL "SELECT NEW ...AssessmentAttemptSummary(...)"
    // projections: status/resultStatus are plain String columns on the JPA
    // entities (enum conversion normally happens at the domain-mapper layer,
    // not on the entity), so this overload accepts the raw strings straight
    // out of the query and converts them here, keeping the record itself
    // properly enum-typed for every other caller.
    public AssessmentAttemptSummary(
            UUID candidateId, UUID assessmentId, String candidateStatus, UUID sessionId,
            Instant startedAt, Instant submittedAt, String status, Boolean flagged,
            String flagReason, BigDecimal totalScore, BigDecimal scoringScaleMin,
            BigDecimal scoringScaleMax, UUID rubricResultBandId, String rubricResultBandCode,
            String rubricResultBandName, String resultStatus, String deletedReason) {
        this(
            candidateId, assessmentId,
            candidateStatus == null ? null : AssessmentCandidateStatus.valueOf(candidateStatus),
            sessionId, startedAt, submittedAt, AssessmentSessionStatus.valueOf(status),
            Boolean.TRUE.equals(flagged), flagReason, totalScore, scoringScaleMin, scoringScaleMax,
            rubricResultBandId, rubricResultBandCode, rubricResultBandName,
            resultStatus == null ? null : AssessmentCandidateResultStatus.valueOf(resultStatus),
            deletedReason
        );
    }
}

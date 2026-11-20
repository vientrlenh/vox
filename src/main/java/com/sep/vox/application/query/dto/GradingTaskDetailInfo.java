package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;

/**
 * The teacher's grading screen.
 *
 * <p>For a {@code CENTRALIZED} assessment, {@code studentName}/{@code className} are always
 * {@code null} — blind grading guarantees fairness. A class test carries them, because the
 * grader is the very teacher who teaches that class.
 *
 * <p>{@code allowedOutcomes} is decided by the BE according to {@code GradingRoundPolicy}: the FE builds
 * exactly the buttons allowed in this round and does not derive them from {@code roundType} — deriving them
 * in two places means the two places drift apart sooner or later.
 */
public record GradingTaskDetailInfo(
    UUID assignmentId,
    UUID candidateResultId,
    String resultCode,
    String assessmentName,
    String roundType,
    String assignmentStatus,
    AssessmentCandidateResultStatus resultStatus,
    boolean flagged,
    String flagReason,
    BigDecimal currentTotalScore,
    /** The score when the assignment was given — the reference point so the teacher knows what they are editing from. */
    BigDecimal scoreBefore,
    Instant deadlineAt,
    boolean overdue,
    /** Read-only flag decided by the BE: true only when the assignment is still ASSIGNED and the attempt is in the right round. */
    boolean editable,
    List<String> allowedOutcomes,
    /** Only in the APPEAL round: the reason the student gave in the appeal. */
    String appealReason,
    List<GradingTaskItemInfo> items,
    List<GradingCriterionMetaInfo> criteria,
    /** Only has a value for class tests; always null for a centralized assessment. */
    String studentName,
    /** Only has a value for class tests; always null for a centralized assessment. */
    String className,
    /** The session being graded. Two attempts of the same student differ here. */
    UUID sessionId,
    /** Which attempt this is, counted from 1 by the time the student started the assessment. */
    int attemptNo,
    /** Total number of attempts that student has made on this assessment. 1 means no retake. */
    int attemptCount
) {
}

package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;

/**
 * The teacher's queue -- ONE list for all four rounds, told apart by
 * {@code roundType}.
 *
 * <p><strong>Anonymity is a rule of {@code CENTRALIZED} assessments</strong>, not a general
 * rule: there {@code studentName}/{@code className} are always {@code null} to guarantee blind
 * grading. A class test is the opposite -- the grader is the very teacher who teaches that class,
 * and they need to know whom they are grading. See {@code JpaAssessmentGradingQueryRepository}.
 */
public record GradingTaskInfo(
    UUID assignmentId,
    UUID candidateResultId,
    String resultCode,
    String assessmentName,
    int partCount,
    String roundType,
    String status,
    AssessmentCandidateResultStatus resultStatus,
    /** The current score of the attempt — in the spot-check/appeal rounds this is the published score. */
    BigDecimal currentScore,
    boolean flagged,
    Instant assignedAt,
    Instant deadlineAt,
    boolean overdue,
    /** Only has a value for class tests; always null for a centralized assessment. */
    String studentName,
    /** Only has a value for class tests; always null for a centralized assessment. */
    String className,
    /** The session that produced this attempt. Two attempts of the same student differ here. */
    UUID sessionId,
    /** Which attempt this is, counted from 1 by the time the student started the assessment. */
    int attemptNo,
    /** Total number of attempts that student has made on this assessment. 1 means no retake. */
    int attemptCount
) {
}

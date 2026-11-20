package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;

/**
 * A row of the school admin's assignment table. The admin sees the student's name normally —
 * anonymity only applies on the teacher's side.
 *
 * <p>A null {@code assignmentId} means the attempt has no open assignment. The attempt may
 * have gone through several earlier rounds; the closed rounds live on the history screen and do not
 * crowd into this coordination table.
 *
 * <p>One row is ONE ATTEMPT, not one student: a student who retakes has that many rows with the
 * same name. {@code attemptNo}/{@code attemptCount} are the only thing that tells them apart
 * on screen — without them the grader would see two identical rows.
 */
public record GradingAssignmentRowInfo(
    UUID candidateResultId,
    String resultCode,
    String studentName,
    String className,
    String assessmentName,
    AssessmentCandidateResultStatus resultStatus,
    BigDecimal totalScore,
    boolean flagged,
    UUID assignmentId,
    UUID teacherId,
    String teacherName,
    String roundType,
    String assignmentStatus,
    String outcome,
    Instant assignedAt,
    Instant completedAt,
    Instant deadlineAt,
    boolean overdue,
    /** There is an unfinished appeal — the admin needs to know before assigning another round. */
    boolean hasOpenAppeal,
    /** The session that produced this attempt. Two attempts of the same student differ here. */
    UUID sessionId,
    /** Which attempt this is, counted from 1 by the time the student started the assessment. */
    int attemptNo,
    /** Total number of attempts that student has made on this assessment. 1 means no retake. */
    int attemptCount
) {
}

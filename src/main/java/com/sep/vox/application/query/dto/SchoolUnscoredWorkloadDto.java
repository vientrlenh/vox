package com.sep.vox.application.query.dto;

import java.time.Instant;

/**
 * Submissions that are finished but the student has NO score yet, split by what is blocking them.
 *
 * <p>The five buckets are MUTUALLY EXCLUSIVE and exhaustive -- they add up to exactly {@link #total()}.
 * That is the condition for the card on the overview page to add five rows into one number without
 * counting anyone twice.
 *
 * <p>Do NOT use {@code gradingStats.unassigned} for the "waiting for assignment" bucket even though it
 * sounds identical: that one counts submissions ELIGIBLE for one more grading round, a set that also
 * includes RELEASED / INVALID / APPEALED -- that is, every submission whose score is already published
 * is in it. Using it by mistake turns "students without a score" into "almost every submission of the school".
 */
public record SchoolUnscoredWorkloadDto(
    /** AI grading failed, nobody has touched it, and the school still has AI re-grade attempts. */
    int aiFailedRetryLeft,
    /** AI grading failed and the AI attempts are used up -- only manual grading is left. */
    int aiFailedNoRetryLeft,
    /** A result row already waits for a grader but has not been assigned to anyone. */
    int awaitingAssignment,
    /** Assigned and past the grading deadline. */
    int assignedOverdue,
    /** Assigned and still within the deadline. */
    int assignedInProgress,
    /** The submission time of the LONGEST-waiting submission; null when no submission is left without a score. */
    Instant oldestSubmittedAt,
    /** The number of DISTINCT assessments that have submissions without a score. */
    int assessmentCount
) {

    public int total() {
        return aiFailedRetryLeft + aiFailedNoRetryLeft + awaitingAssignment + assignedOverdue
            + assignedInProgress;
    }

    /** The total number of submissions with failed AI grading that nobody has handled -- the two sub-buckets combined into one row on the UI. */
    public int aiFailed() {
        return aiFailedRetryLeft + aiFailedNoRetryLeft;
    }

    public static SchoolUnscoredWorkloadDto empty() {
        return new SchoolUnscoredWorkloadDto(0, 0, 0, 0, 0, null, 0);
    }
}

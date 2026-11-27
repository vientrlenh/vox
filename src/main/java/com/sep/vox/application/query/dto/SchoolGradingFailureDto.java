package com.sep.vox.application.query.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * An attempt whose AI grading failed that NOBODY has handled yet, seen from the school's side.
 *
 * <p>No score: a {@code GRADING_FAILED} session never produced an {@code assessment_candidate_results}
 * row. What replaces it are the fields that help decide between the two exits -- ask the AI to regrade or assign a
 * teacher to grade by hand.
 *
 * <p>Two DIFFERENT counters, do not merge them:
 *
 * <ul>
 *   <li>{@code aiRetryCount} is the number of times the grading service retried by itself inside ONE attempt, taken from the Kafka
 *       payload. It speaks to how persistent the incident is, not to who pressed what.
 *   <li>{@code schoolRetryLeft} is derived from {@code assessment_sessions.school_regrade_count} -- the school's allowance of
 *       one AI regrade request. This is what decides which button can still be pressed.
 * </ul>
 */
public record SchoolGradingFailureDto(
    UUID sessionId,
    UUID assessmentId,
    String assessmentCode,
    String assessmentName,
    String candidateName,
    /** null when the student has not been placed in any active class. */
    String className,
    /** The submission time -- a failed session has no other trustworthy timestamp to order by. */
    Instant failedAt,
    /** The raw error message; null for a session marked failed through the DLT path. */
    String error,
    /** How many times the grading service retried by itself before giving up; null when unknown. */
    Integer aiRetryCount,
    /** Whether the school still has AI re-grade attempts left for this session. */
    boolean schoolRetryLeft
) {
}

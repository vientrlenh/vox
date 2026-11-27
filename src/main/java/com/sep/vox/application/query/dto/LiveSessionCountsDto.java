package com.sep.vox.application.query.dto;

/**
 * A snapshot of the assessment sessions at EXACTLY the moment of the call -- independent of the time
 * window being viewed on the dashboard.
 *
 * <p>Kept apart from {@link GradingOutcomeBucketDto} because the two numbers answer two different
 * questions: the daily series is "is the AI grading path healthy over the last 14 days", while this
 * record is "what is running right now". Putting them in one query would force one of them to accept a
 * time window it does not need.
 */
public record LiveSessionCountsDto(
    long sessionsInProgress,
    long assessmentsInProgress,
    long gradingQueueDepth
) {
}

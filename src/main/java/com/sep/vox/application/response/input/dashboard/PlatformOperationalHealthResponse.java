package com.sep.vox.application.response.input.dashboard;

import java.util.List;

/**
 * The operational health of the platform for the system admin dashboard.
 *
 * <p>Deliberately mixes two kinds of time, and the client must display it exactly that way:
 *
 * <ul>
 *   <li>{@code sessionsInProgress}, {@code assessmentsInProgress}, {@code gradingQueueDepth} are a snapshot
 *       RIGHT AT THE TIME OF THE CALL -- they do not change with the time window the user selects. "6 assessments
 *       are running now" changing when you click to last month is meaningless.
 *   <li>{@code graded}, {@code gradingFailed}, {@code successRatePercent}, {@code daily} belong to the
 *       window being viewed.
 * </ul>
 */
public record PlatformOperationalHealthResponse(
    long sessionsInProgress,
    long assessmentsInProgress,
    long gradingQueueDepth,
    long graded,
    long gradingFailed,
    /**
     * {@code null} when the window has no session that finished grading or failed grading -- 0% and "no
     * data" are two different things, and a system that has not run any session does not have a
     * 0% success rate.
     */
    Double successRatePercent,
    /** Continuous by day, old -> new; a day without any session has been returned as 0. */
    List<GradingOutcomeBucketResponse> daily
) {
}

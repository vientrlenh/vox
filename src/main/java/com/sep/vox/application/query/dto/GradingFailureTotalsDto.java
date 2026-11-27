package com.sep.vox.application.query.dto;

/**
 * The summary strip at the top of the failed-grading-session triage page.
 *
 * <p>Computed with a query of its own rather than by adding up the group list: the group list has a
 * row limit, and {@code schoolCount} is the number of DISTINCT schools across the whole failure set --
 * adding up the school counts of each group would double-count exactly the schools hit by several causes.
 *
 * @param sessionCount   must match the "AI grading failed sessions" tile of the overview page exactly; the same
 *                       predicate, the same window by {@code submitted_at}
 * @param causeCount     number of cause groups, including the "unknown cause" group if there is one
 * @param schoolCount    number of distinct schools affected
 * @param retryableCount number of sessions that can be graded again -- the number the bulk button must
 *                       show, so the user knows the blocked part in advance instead of getting a batch of errors after clicking
 */
public record GradingFailureTotalsDto(
    long sessionCount,
    long causeCount,
    long schoolCount,
    long retryableCount
) {
}

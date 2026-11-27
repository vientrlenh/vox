package com.sep.vox.application.response.input.dashboard;

import java.util.List;

/**
 * One page of submissions with failed AI grading, with the counts of the two quota groups.
 *
 * <p>{@code retryLeftCount}/{@code noRetryLeftCount} HAVE the assessment filter applied but NOT the quota
 * filter -- they sit on the two filter buttons themselves so they must predict the result of the click correctly.
 *
 * <p>When no assessment is filtered, these two numbers match {@code unscored.aiFailedRetryLeft} and
 * {@code unscored.aiFailedNoRetryLeft} on the overview page exactly: one CTE produces both.
 */
public record SchoolGradingFailurePageResponse(
    List<SchoolGradingFailureResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    int retryLeftCount,
    int noRetryLeftCount
) {
}

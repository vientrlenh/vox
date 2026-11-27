package com.sep.vox.application.response.input.dashboard;

import java.util.List;

import com.sep.vox.application.query.dto.GradingFailureGroupDto;

/**
 * The triage page for failed grading sessions: the summary strip plus the list of cause groups.
 *
 * @param groupsTruncated the number of groups that exceeded the cap and were cut. Stated instead of cutting silently, because it is a DIAGNOSTIC
 *                        SIGNAL: if error-message normalization is not working, the number of groups swells to almost
 *                        the number of sessions, and grouping no longer helps -- the function
 *                        {@code vox_grading_error_signature} needs fixing, not the cap loosening.
 */
public record GradingFailureOverviewResponse(
    long sessionCount,
    long causeCount,
    long schoolCount,
    long retryableCount,
    List<GradingFailureGroupDto> groups,
    long groupsTruncated
) {
}

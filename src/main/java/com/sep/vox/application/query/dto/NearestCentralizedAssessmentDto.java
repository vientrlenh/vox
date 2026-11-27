package com.sep.vox.application.query.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * The centralized (CENTRALIZED) assessment of a school that is nearest to the current time -- used for
 * the overview panel on the dashboard. {@code null} if the school has no centralized assessment (still in effect).
 */
public record NearestCentralizedAssessmentDto(
    UUID assessmentId,
    String code,
    String name,
    String status,
    Instant openAt,
    Instant closeAt,
    long totalCandidates,
    long absentCandidates
) {
}

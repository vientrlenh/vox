package com.sep.vox.application.port.input.query.assessmentgrading;

import java.util.UUID;

/**
 * @param kind the assessment kind; {@code null} is read by the use case as {@code CENTRALIZED} -- the same
 *             rule as {@link SearchGradingAssignmentsQuery} so that the table and the number cards at the top of the screen
 *             never count over two different sets.
 */
public record ViewGradingStatsQuery(
    UUID assessmentId,
    UUID scheduleId,
    String kind
) {
}

package com.sep.vox.application.port.input.query.dashboard;

import java.util.UUID;

/**
 * Filters the list of failed-AI-grading submissions that nobody has handled, for the logged-in school.
 *
 * <p>No {@code schoolId}: the scope comes from the logged-in user. Accepting it from the client would open a
 * way for one school's admin to read another school's submissions.
 *
 * @param retryLeft true = only submissions with AI re-grade attempts left, false = only submissions with none left, null =
 *                  no filter. Three states and not an on/off flag, because "no filter" is the default
 *                  and very different from "only submissions with no attempts left".
 */
public record SearchSchoolGradingFailuresQuery(
    UUID assessmentId,
    Boolean retryLeft,
    int page,
    int size
) {
}

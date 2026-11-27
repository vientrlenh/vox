package com.sep.vox.application.port.input.query.assessmentappeal;

import java.util.UUID;

/**
 * Appeal-grading candidates for ONE appeal.
 *
 * <p>Needs {@code appealId} because the conflict-of-interest flag is computed against the submission of that appeal, not
 * against the school.
 */
public record ViewAssignableReviewersQuery(
    UUID appealId,
    String keyword
) {
}

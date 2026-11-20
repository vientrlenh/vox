package com.sep.vox.application.query.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * The appeal grader of an appeal -- only ONE person, and the data is taken from the
 * {@code assessment_grading_assignments} row of the {@code APPEAL} round, not from a separate table.
 *
 * <p>There is no longer a "proposed score": with a single grader there is nothing to compare against, and
 * the score they submit goes straight into the assessment result.
 */
public record AppealReviewerInfo(
    UUID assignmentId,
    UUID reviewerId,
    String reviewerName,
    /** ASSIGNED | COMPLETED */
    String status,
    /** UPHELD | REGRADED | DECLINED; null khi chưa nộp. */
    String outcome,
    Instant assignedAt,
    Instant completedAt,
    Instant deadlineAt,
    boolean overdue
) {
}

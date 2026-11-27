package com.sep.vox.application.port.input.command.assessmentappeal;

import java.time.Instant;
import java.util.UUID;

/**
 * Assigns ONE appeal grader to an appeal.
 *
 * @param overrideReason the reason for deliberately assigning someone who has previously given a manual score for this submission.
 *        Not passed = keep the conflict-of-interest rule and the use case will refuse. Passed =
 *        the admin accepts the risk (a small school without enough teachers) and the reason goes into the audit.
 * @param deadlineAt a separate grading deadline for the appeal round; if empty, the appeal's own deadline is used
 */
public record AssignAssessmentAppealReviewerCommand(
    UUID appealId,
    UUID reviewerId,
    String overrideReason,
    Instant deadlineAt
) {
}

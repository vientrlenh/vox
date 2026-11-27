package com.sep.vox.application.event;

import java.time.Instant;
import java.util.UUID;

/**
 * An appeal has been approved and a grader has been assigned.
 *
 * <p>Published at the ASSIGNMENT step, not at the approval step: "approved" and "assigned"
 * can be days apart, and the message worth sending to the student is the second one,
 * because it carries a concrete deadline.
 */
public record AssessmentAppealApprovedPayloadV1(
    UUID appealId,
    UUID studentId,
    String assessmentName,
    Instant deadline
) {

}

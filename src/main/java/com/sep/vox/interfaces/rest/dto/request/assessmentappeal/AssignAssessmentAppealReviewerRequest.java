package com.sep.vox.interfaces.rest.dto.request.assessmentappeal;

import com.sep.vox.application.port.input.command.assessmentappeal.AssignAssessmentAppealReviewerCommand;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Assigns ONE appeal grader.
 *
 * @param overrideReason empty = keep the conflict-of-interest rule (a person who already graded this submission
 *        is rejected). Has a value = the admin accepts assigning them anyway, and the reason is
 *        stored on the appeal.
 */
public record AssignAssessmentAppealReviewerRequest(
    @NotNull(message = "The appeal grader is missing")
    UUID reviewerId,

    @Size(max = 1024, message = "Reason must not exceed 1024 characters")
    String overrideReason,

    Instant deadlineAt
) {

    public static AssignAssessmentAppealReviewerCommand toCommand(
            UUID appealId, AssignAssessmentAppealReviewerRequest request) {
        return new AssignAssessmentAppealReviewerCommand(
            appealId, request.reviewerId(), request.overrideReason(), request.deadlineAt());
    }
}

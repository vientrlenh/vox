package com.sep.vox.application.port.input.command.assessmentsession;

import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;

/**
 * @param gradingFailure details of the failed grading; only meaningful when {@code status = GRADING_FAILED}, and
 *                       {@code null} there means "failed but we do not know why" (the DLT branch), and
 *                       not "not filled in yet".
 */
public record UpdateAssessmentSessionStatusCommand(
    UUID sessionId,
    AssessmentSessionStatus status,
    GradingFailure gradingFailure
) {

    /**
     * @param error      the raw message of the grading service, verbatim -- it is the only thing that can tell
     *                   one service incident apart from thousands of scattered errors
     * @param retryCount the number of times the service retried before giving up
     */
    public record GradingFailure(String error, Integer retryCount) {
    }

    /**
     * The compact form for every status transition that is NOT a grading failure -- most callers (re-entering the exam room,
     * time up, submitting) have nothing to say about grading, and making them pass {@code null} only
     * adds noise at the call site.
     */
    public UpdateAssessmentSessionStatusCommand(UUID sessionId, AssessmentSessionStatus status) {
        this(sessionId, status, null);
    }

    public static UpdateAssessmentSessionStatusCommand gradingFailed(UUID sessionId, String error, Integer retryCount) {
        return new UpdateAssessmentSessionStatusCommand(
            sessionId,
            AssessmentSessionStatus.GRADING_FAILED,
            new GradingFailure(error, retryCount)
        );
    }

    /** Failed grading with no reason to store -- the message has run out of retries and fallen to the DLT. */
    public static UpdateAssessmentSessionStatusCommand gradingFailedWithoutReason(UUID sessionId) {
        return new UpdateAssessmentSessionStatusCommand(sessionId, AssessmentSessionStatus.GRADING_FAILED, null);
    }
}

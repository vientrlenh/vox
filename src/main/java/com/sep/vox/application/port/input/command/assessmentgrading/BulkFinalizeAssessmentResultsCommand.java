package com.sep.vox.application.port.input.command.assessmentgrading;

import java.util.UUID;

/**
 * Closes out an assessment: publishes the submissions still waiting for a grader using the AI scores currently held.
 *
 * @param releasePendingWithAiScores the admin's confirmation that submissions nobody has graded may be
 *        published with the AI scores currently held. Must be {@code true} when the preview still has blocking submissions
 *        -- {@code false} by default so nobody accidentally publishes scores that no person has looked at.
 */
public record BulkFinalizeAssessmentResultsCommand(
    UUID assessmentId,
    boolean releasePendingWithAiScores
) {
}

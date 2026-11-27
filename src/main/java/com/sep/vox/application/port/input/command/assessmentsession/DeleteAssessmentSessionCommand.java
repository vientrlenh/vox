package com.sep.vox.application.port.input.command.assessmentsession;

import java.util.UUID;

/**
 * Soft-deletes an assessment session.
 *
 * <p>{@code reason} is REQUIRED (the use case checks it itself, it may not be empty): deleting an assessment submission is an operation that
 * must be justifiable to the student and their parents, so the reason is stored with the data row and not only
 * in the log.
 */
public record DeleteAssessmentSessionCommand(
    UUID sessionId,
    String reason
) {
}

package com.sep.vox.application.port.input.command.assessmentgrading;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Sets / changes the grading deadline for one or more assignments.
 *
 * @param deadlineAt {@code null} = remove the deadline (the submission is no longer counted as overdue)
 */
public record SetGradingDeadlineCommand(
    List<UUID> assignmentIds,
    Instant deadlineAt
) {
}

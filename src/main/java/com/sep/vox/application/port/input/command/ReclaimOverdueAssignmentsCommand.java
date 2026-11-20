package com.sep.vox.application.port.input.command;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Reclaims overdue assignments and (optionally) reassigns them right away to another group.
 *
 * @param assignmentIds empty = reclaim every overdue assignment within the scope of
 *        {@code assessmentId}; with a value = only exactly the selected rows
 * @param reassignToTeacherIds empty = reclaim only, the attempts go back to the unassigned queue
 */
public record ReclaimOverdueAssignmentsCommand(
    UUID assessmentId,
    List<UUID> assignmentIds,
    List<UUID> reassignToTeacherIds,
    Instant newDeadlineAt
) {
}

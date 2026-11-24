package com.sep.vox.interfaces.rest.dto.request.assessmentgrading;

import com.sep.vox.application.port.input.command.assessmentgrading.ReclaimOverdueAssignmentsCommand;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @param assignmentIds empty = reclaim every overdue assignment within the scope of
 *        {@code assessmentId} (both empty = the whole school)
 * @param reassignToTeacherIds empty = reclaim only, the attempts go back to the unassigned queue
 */
public record ReclaimOverdueAssignmentsRequest(
    UUID assessmentId,
    List<UUID> assignmentIds,
    List<UUID> reassignToTeacherIds,
    Instant newDeadlineAt
) {

    public static ReclaimOverdueAssignmentsCommand toCommand(ReclaimOverdueAssignmentsRequest request) {
        return new ReclaimOverdueAssignmentsCommand(
            request.assessmentId(),
            request.assignmentIds(),
            request.reassignToTeacherIds(),
            request.newDeadlineAt()
        );
    }
}

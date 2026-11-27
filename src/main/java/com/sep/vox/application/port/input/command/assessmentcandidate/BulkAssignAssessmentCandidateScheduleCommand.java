package com.sep.vox.application.port.input.command.assessmentcandidate;

import java.util.List;
import java.util.UUID;

/** A null {@code scheduleId} means removing the whole group from the schedule, like the endpoint that places candidates one by one. */
public record BulkAssignAssessmentCandidateScheduleCommand(
    UUID assessmentId,
    List<UUID> candidateIds,
    UUID scheduleId
) {
}

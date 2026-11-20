package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record UpdateAssessmentCandidatesAttendanceCommand(
    UUID scheduleId,
    List<UUID> candidateIds
) {
}

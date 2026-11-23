package com.sep.vox.application.port.input.command.assessmentcandidate;

import java.util.List;
import java.util.UUID;

public record UpdateAssessmentCandidatesAttendanceCommand(
    UUID scheduleId,
    List<UUID> candidateIds
) {
}

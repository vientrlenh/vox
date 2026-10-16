package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record AssignAssessmentCandidateScheduleCommand(
    UUID assessmentId,
    UUID candidateId,
    UUID scheduleId
) {
}

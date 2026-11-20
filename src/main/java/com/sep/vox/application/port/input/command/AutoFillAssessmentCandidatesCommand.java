package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record AutoFillAssessmentCandidatesCommand(
    UUID assessmentId,
    List<UUID> scheduleIds
) {
}

package com.sep.vox.application.port.input.command.assessment;

import java.util.UUID;

public record UpdateAssessmentStatusCommand(
    UUID assessmentId,
    String action,
    String note
) {
}

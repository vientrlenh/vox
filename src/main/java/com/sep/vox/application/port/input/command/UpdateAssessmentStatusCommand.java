package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateAssessmentStatusCommand(
    UUID assessmentId,
    String action,
    String note
) {
}

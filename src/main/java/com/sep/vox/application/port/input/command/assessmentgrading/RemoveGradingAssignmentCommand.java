package com.sep.vox.application.port.input.command.assessmentgrading;

import java.util.UUID;

public record RemoveGradingAssignmentCommand(
    UUID assignmentId
) {
}

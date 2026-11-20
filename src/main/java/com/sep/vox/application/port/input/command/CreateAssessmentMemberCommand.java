package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CreateAssessmentMemberCommand(
    UUID assessmentId,
    UUID userId,
    String role
) {
}

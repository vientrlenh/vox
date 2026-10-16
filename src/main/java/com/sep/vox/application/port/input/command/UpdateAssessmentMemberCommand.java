package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateAssessmentMemberCommand(
    UUID assessmentId,
    UUID memberId,
    String role
) {
}

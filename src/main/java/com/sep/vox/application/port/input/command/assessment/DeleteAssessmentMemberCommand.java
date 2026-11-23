package com.sep.vox.application.port.input.command.assessment;

import java.util.UUID;

public record DeleteAssessmentMemberCommand(
    UUID assessmentId,
    UUID memberId
) {
}

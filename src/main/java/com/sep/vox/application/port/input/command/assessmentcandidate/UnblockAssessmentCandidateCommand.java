package com.sep.vox.application.port.input.command.assessmentcandidate;

import java.util.UUID;

public record UnblockAssessmentCandidateCommand(
    UUID candidateId,
    String reason
) {
}

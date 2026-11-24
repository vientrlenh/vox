package com.sep.vox.application.port.input.command.assessmentcandidate;

import java.util.UUID;

public record AddAssessmentCandidateCommand(
    UUID assessmentId,
    UUID studentId
) {
}

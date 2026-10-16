package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record AddAssessmentCandidateCommand(
    UUID assessmentId,
    UUID studentId
) {
}

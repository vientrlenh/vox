package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record ImportAssessmentCandidatesFromGradeCommand(
    UUID assessmentId,
    UUID schoolGradeId
) {
}

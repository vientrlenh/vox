package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record ImportAssessmentCandidatesFromClassCommand(
    UUID assessmentId,
    UUID schoolClassId
) {
}

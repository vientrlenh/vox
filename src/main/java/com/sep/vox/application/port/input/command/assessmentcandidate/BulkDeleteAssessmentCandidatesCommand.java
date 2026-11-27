package com.sep.vox.application.port.input.command.assessmentcandidate;

import java.util.List;
import java.util.UUID;

/** Removes a whole group of candidates from the assessment in ONE transaction -- for the screen where many candidates are ticked. */
public record BulkDeleteAssessmentCandidatesCommand(
    UUID assessmentId,
    List<UUID> candidateIds
) {
}

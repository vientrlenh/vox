package com.sep.vox.application.port.input.command;

import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;

public record UpdateAssessmentCandidateStatusCommand(
    UUID candidateId,
    AssessmentCandidateStatus status
) {
}

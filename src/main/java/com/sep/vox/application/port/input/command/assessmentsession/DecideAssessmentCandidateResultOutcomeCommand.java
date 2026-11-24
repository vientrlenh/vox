package com.sep.vox.application.port.input.command.assessmentsession;

import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;

public record DecideAssessmentCandidateResultOutcomeCommand(
    UUID candidateResultId,
    AssessmentCandidateResultStatus decision
) {
}

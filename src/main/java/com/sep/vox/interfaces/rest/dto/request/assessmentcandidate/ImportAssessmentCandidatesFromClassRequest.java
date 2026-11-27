package com.sep.vox.interfaces.rest.dto.request.assessmentcandidate;

import com.sep.vox.application.port.input.command.assessmentcandidate.ImportAssessmentCandidatesFromClassCommand;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ImportAssessmentCandidatesFromClassRequest(
    @NotNull(message = "Class is required")
    UUID schoolClassId
) {

    public static ImportAssessmentCandidatesFromClassCommand toCommand(UUID assessmentId,
            ImportAssessmentCandidatesFromClassRequest request) {
        return new ImportAssessmentCandidatesFromClassCommand(assessmentId, request.schoolClassId());
    }
}

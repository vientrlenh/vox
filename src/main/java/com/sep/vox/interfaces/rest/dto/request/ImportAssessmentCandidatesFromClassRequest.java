package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import com.sep.vox.application.port.input.command.ImportAssessmentCandidatesFromClassCommand;

import jakarta.validation.constraints.NotNull;

public record ImportAssessmentCandidatesFromClassRequest(
    @NotNull(message = "Lớp học là bắt buộc")
    UUID schoolClassId
) {

    public static ImportAssessmentCandidatesFromClassCommand toCommand(UUID assessmentId, ImportAssessmentCandidatesFromClassRequest request) {
        return new ImportAssessmentCandidatesFromClassCommand(assessmentId, request.schoolClassId());
    }
}

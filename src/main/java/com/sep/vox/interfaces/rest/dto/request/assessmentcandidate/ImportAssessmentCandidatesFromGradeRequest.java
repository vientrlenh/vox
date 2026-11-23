package com.sep.vox.interfaces.rest.dto.request.assessmentcandidate;

import com.sep.vox.application.port.input.command.assessmentcandidate.ImportAssessmentCandidatesFromGradeCommand;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ImportAssessmentCandidatesFromGradeRequest(
    @NotNull(message = "Khối là bắt buộc")
    UUID schoolGradeId
) {

    public static ImportAssessmentCandidatesFromGradeCommand toCommand(UUID assessmentId,
            ImportAssessmentCandidatesFromGradeRequest request) {
        return new ImportAssessmentCandidatesFromGradeCommand(assessmentId, request.schoolGradeId());
    }
}

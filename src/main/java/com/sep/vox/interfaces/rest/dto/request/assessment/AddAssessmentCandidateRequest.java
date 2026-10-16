package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.UUID;

import com.sep.vox.application.port.input.command.AddAssessmentCandidateCommand;

import jakarta.validation.constraints.NotNull;

public record AddAssessmentCandidateRequest(
    @NotNull(message = "Học sinh là bắt buộc")
    UUID studentId
) {

    public static AddAssessmentCandidateCommand toCommand(UUID assessmentId, AddAssessmentCandidateRequest request) {
        return new AddAssessmentCandidateCommand(assessmentId, request.studentId());
    }
}

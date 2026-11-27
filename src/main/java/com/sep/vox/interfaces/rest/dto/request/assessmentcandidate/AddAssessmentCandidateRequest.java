package com.sep.vox.interfaces.rest.dto.request.assessmentcandidate;

import com.sep.vox.application.port.input.command.assessmentcandidate.AddAssessmentCandidateCommand;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AddAssessmentCandidateRequest(
    @NotNull(message = "Student is required")
    UUID studentId
) {

    public static AddAssessmentCandidateCommand toCommand(UUID assessmentId, AddAssessmentCandidateRequest request) {
        return new AddAssessmentCandidateCommand(assessmentId, request.studentId());
    }
}

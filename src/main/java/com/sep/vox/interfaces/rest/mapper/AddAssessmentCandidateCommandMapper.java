package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.AddAssessmentCandidateCommand;
import com.sep.vox.interfaces.rest.dto.request.AddAssessmentCandidateRequest;

public final class AddAssessmentCandidateCommandMapper {

    private AddAssessmentCandidateCommandMapper() {
    }

    public static AddAssessmentCandidateCommand fromRequest(UUID assessmentId, AddAssessmentCandidateRequest request) {
        return new AddAssessmentCandidateCommand(assessmentId, request.studentId());
    }
}

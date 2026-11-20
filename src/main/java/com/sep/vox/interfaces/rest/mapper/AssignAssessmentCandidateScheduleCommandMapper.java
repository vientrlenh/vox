package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.AssignAssessmentCandidateScheduleCommand;
import com.sep.vox.interfaces.rest.dto.request.AssignAssessmentCandidateScheduleRequest;

public final class AssignAssessmentCandidateScheduleCommandMapper {

    private AssignAssessmentCandidateScheduleCommandMapper() {
    }

    public static AssignAssessmentCandidateScheduleCommand fromRequest(UUID assessmentId, UUID candidateId,
            AssignAssessmentCandidateScheduleRequest request) {
        return new AssignAssessmentCandidateScheduleCommand(assessmentId, candidateId, request.scheduleId());
    }
}

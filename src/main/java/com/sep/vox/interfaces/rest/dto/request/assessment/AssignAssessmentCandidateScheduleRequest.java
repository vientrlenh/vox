package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.UUID;

import com.sep.vox.application.port.input.command.AssignAssessmentCandidateScheduleCommand;

public record AssignAssessmentCandidateScheduleRequest(
    UUID scheduleId
) {

    public static AssignAssessmentCandidateScheduleCommand toCommand(UUID assessmentId, UUID candidateId, AssignAssessmentCandidateScheduleRequest request) {
        return new AssignAssessmentCandidateScheduleCommand(assessmentId, candidateId, request.scheduleId());
    }
}

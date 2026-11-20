package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.BulkAssignAssessmentCandidateScheduleCommand;
import com.sep.vox.interfaces.rest.dto.request.BulkAssignAssessmentCandidateScheduleRequest;

public final class BulkAssignAssessmentCandidateScheduleCommandMapper {

    private BulkAssignAssessmentCandidateScheduleCommandMapper() {
    }

    public static BulkAssignAssessmentCandidateScheduleCommand fromRequest(UUID assessmentId,
            BulkAssignAssessmentCandidateScheduleRequest request) {
        return new BulkAssignAssessmentCandidateScheduleCommand(assessmentId, request.candidateIds(), request.scheduleId());
    }
}

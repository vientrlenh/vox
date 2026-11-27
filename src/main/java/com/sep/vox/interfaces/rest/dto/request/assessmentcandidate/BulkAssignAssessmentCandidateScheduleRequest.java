package com.sep.vox.interfaces.rest.dto.request.assessmentcandidate;

import com.sep.vox.application.port.input.command.assessmentcandidate.BulkAssignAssessmentCandidateScheduleCommand;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

/** {@code scheduleId} is deliberately not required: empty means removing the whole group from the exam slot. */
public record BulkAssignAssessmentCandidateScheduleRequest(
    @NotEmpty(message = "Candidate list must not be blank")
    List<UUID> candidateIds,
    UUID scheduleId
) {

    public static BulkAssignAssessmentCandidateScheduleCommand toCommand(UUID assessmentId,
            BulkAssignAssessmentCandidateScheduleRequest request) {
        return new BulkAssignAssessmentCandidateScheduleCommand(assessmentId, request.candidateIds(), request.scheduleId());
    }
}

package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.AutoFillAssessmentCandidatesCommand;

public record AutoFillAssessmentCandidatesRequest(
    List<UUID> scheduleIds
) {

    public static AutoFillAssessmentCandidatesCommand toCommand(UUID assessmentId, AutoFillAssessmentCandidatesRequest request) {
        var scheduleIds = request == null ? null : request.scheduleIds();
        return new AutoFillAssessmentCandidatesCommand(assessmentId, scheduleIds);
    }
}

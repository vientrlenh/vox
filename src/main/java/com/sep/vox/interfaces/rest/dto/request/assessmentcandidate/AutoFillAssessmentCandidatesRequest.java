package com.sep.vox.interfaces.rest.dto.request.assessmentcandidate;

import com.sep.vox.application.port.input.command.assessmentcandidate.AutoFillAssessmentCandidatesCommand;
import java.util.List;
import java.util.UUID;

public record AutoFillAssessmentCandidatesRequest(
    List<UUID> scheduleIds
) {

    public static AutoFillAssessmentCandidatesCommand toCommand(UUID assessmentId, AutoFillAssessmentCandidatesRequest request) {
        List<UUID> scheduleIds = request == null ? null : request.scheduleIds();
        return new AutoFillAssessmentCandidatesCommand(assessmentId, scheduleIds);
    }
}

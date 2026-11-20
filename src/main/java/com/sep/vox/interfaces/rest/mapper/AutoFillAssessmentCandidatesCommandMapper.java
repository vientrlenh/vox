package com.sep.vox.interfaces.rest.mapper;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.AutoFillAssessmentCandidatesCommand;
import com.sep.vox.interfaces.rest.dto.request.AutoFillAssessmentCandidatesRequest;

public final class AutoFillAssessmentCandidatesCommandMapper {

    private AutoFillAssessmentCandidatesCommandMapper() {
    }

    public static AutoFillAssessmentCandidatesCommand fromRequest(UUID assessmentId, AutoFillAssessmentCandidatesRequest request) {
        List<UUID> scheduleIds = request == null ? null : request.scheduleIds();
        return new AutoFillAssessmentCandidatesCommand(assessmentId, scheduleIds);
    }
}

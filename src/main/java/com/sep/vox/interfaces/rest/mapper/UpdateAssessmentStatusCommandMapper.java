package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateAssessmentStatusCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateAssessmentStatusRequest;

public final class UpdateAssessmentStatusCommandMapper {

    private UpdateAssessmentStatusCommandMapper() {
    }

    public static UpdateAssessmentStatusCommand fromRequest(UUID assessmentId, UpdateAssessmentStatusRequest request) {
        return new UpdateAssessmentStatusCommand(assessmentId, request.action(), request.note());
    }
}

package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateAssessmentSecurePoolStatusCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateAssessmentSecurePoolStatusRequest;

public final class UpdateAssessmentSecurePoolStatusCommandMapper {

    private UpdateAssessmentSecurePoolStatusCommandMapper() {
    }

    public static UpdateAssessmentSecurePoolStatusCommand fromRequest(UUID assessmentId, UpdateAssessmentSecurePoolStatusRequest request) {
        return new UpdateAssessmentSecurePoolStatusCommand(assessmentId, request.action());
    }
}

package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateAssessmentMemberCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateAssessmentMemberRequest;

public final class CreateAssessmentMemberCommandMapper {

    private CreateAssessmentMemberCommandMapper() {
    }

    public static CreateAssessmentMemberCommand fromRequest(UUID assessmentId, CreateAssessmentMemberRequest request) {
        return new CreateAssessmentMemberCommand(assessmentId, request.userId(), request.role());
    }
}

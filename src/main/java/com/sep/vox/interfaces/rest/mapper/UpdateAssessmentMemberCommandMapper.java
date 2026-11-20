package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateAssessmentMemberCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateAssessmentMemberRequest;

public final class UpdateAssessmentMemberCommandMapper {

    private UpdateAssessmentMemberCommandMapper() {
    }

    public static UpdateAssessmentMemberCommand fromRequest(UUID assessmentId, UUID memberId, UpdateAssessmentMemberRequest request) {
        return new UpdateAssessmentMemberCommand(assessmentId, memberId, request.role());
    }
}

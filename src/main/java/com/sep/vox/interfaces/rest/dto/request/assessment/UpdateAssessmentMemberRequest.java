package com.sep.vox.interfaces.rest.dto.request.assessment;

import com.sep.vox.application.port.input.command.assessment.UpdateAssessmentMemberCommand;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record UpdateAssessmentMemberRequest(
    @NotNull(message = "Role là bắt buộc")
    AssessmentMemberRole role
) {

    public static UpdateAssessmentMemberCommand toCommand(UUID assessmentId, UUID memberId, UpdateAssessmentMemberRequest request) {
        return new UpdateAssessmentMemberCommand(assessmentId, memberId, request.role());
    }
}

package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.UUID;

import com.sep.vox.application.port.input.command.assessment.CreateAssessmentMemberCommand;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;

import jakarta.validation.constraints.NotNull;

public record CreateAssessmentMemberRequest(
    @NotNull(message = "UserId là bắt buộc")
    UUID userId,

    @NotNull(message = "Role là bắt buộc")
    AssessmentMemberRole role
) {

    public static CreateAssessmentMemberCommand toCommand(UUID assessmentId, CreateAssessmentMemberRequest request) {
        return new CreateAssessmentMemberCommand(assessmentId, request.userId(), request.role());
    }
}

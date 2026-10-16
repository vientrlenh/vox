package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateAssessmentMemberCommand;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateAssessmentMemberRequest(
    @NotNull(message = "UserId là bắt buộc")
    UUID userId,

    @Pattern(
        regexp = "CHAIR|AUTHOR|REVIEWER",
        message = "Role không hợp lệ"
    )
    String role
) {

    public static CreateAssessmentMemberCommand toCommand(UUID assessmentId, CreateAssessmentMemberRequest request) {
        return new CreateAssessmentMemberCommand(assessmentId, request.userId(), request.role());
    }
}

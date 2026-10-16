package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateAssessmentMemberCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateAssessmentMemberRequest(
    @NotBlank(message = "Role là bắt buộc")
    @Pattern(
        regexp = "CHAIR|AUTHOR|REVIEWER",
        message = "Role không hợp lệ"
    )
    String role
) {

    public static UpdateAssessmentMemberCommand toCommand(UUID assessmentId, UUID memberId, UpdateAssessmentMemberRequest request) {
        return new UpdateAssessmentMemberCommand(assessmentId, memberId, request.role());
    }
}

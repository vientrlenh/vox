package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateAssessmentSecurePoolStatusCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateAssessmentSecurePoolStatusRequest(
    @NotBlank(message = "Action là bắt buộc")
    @Pattern(
        regexp = "RELEASE",
        message = "Action không hợp lệ"
    )
    String action
) {

    public static UpdateAssessmentSecurePoolStatusCommand toCommand(UUID assessmentId, UpdateAssessmentSecurePoolStatusRequest request) {
        return new UpdateAssessmentSecurePoolStatusCommand(assessmentId, request.action());
    }
}

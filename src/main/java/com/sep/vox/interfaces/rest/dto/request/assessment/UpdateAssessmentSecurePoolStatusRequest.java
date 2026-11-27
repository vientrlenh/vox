package com.sep.vox.interfaces.rest.dto.request.assessment;

import com.sep.vox.application.port.input.command.assessment.UpdateAssessmentSecurePoolStatusCommand;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateAssessmentSecurePoolStatusRequest(
    @NotBlank(message = "Action is required")
    @Pattern(
        regexp = "RELEASE",
        message = "Action is invalid"
    )
    String action
) {

    public static UpdateAssessmentSecurePoolStatusCommand toCommand(UUID assessmentId, UpdateAssessmentSecurePoolStatusRequest request) {
        return new UpdateAssessmentSecurePoolStatusCommand(assessmentId, request.action());
    }
}

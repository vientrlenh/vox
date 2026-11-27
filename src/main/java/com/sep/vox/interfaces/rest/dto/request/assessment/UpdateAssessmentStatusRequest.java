package com.sep.vox.interfaces.rest.dto.request.assessment;

import com.sep.vox.application.port.input.command.assessment.UpdateAssessmentStatusCommand;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateAssessmentStatusRequest(
    @NotBlank(message = "Action is required")
    @Pattern(
        regexp = "SCHEDULE|START|CLOSE|PUBLISH_RESULTS|CANCEL",
        message = "Action is invalid"
    )
    String action,
    String note
) {

    public static UpdateAssessmentStatusCommand toCommand(UUID assessmentId, UpdateAssessmentStatusRequest request) {
        return new UpdateAssessmentStatusCommand(assessmentId, request.action(), request.note());
    }
}

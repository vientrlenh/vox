package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateAssessmentStatusCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateAssessmentStatusRequest(
    @NotBlank(message = "Action là bắt buộc")
    @Pattern(
        regexp = "SCHEDULE|START|CLOSE|PUBLISH_RESULTS|CANCEL",
        message = "Action không hợp lệ"
    )
    String action,
    String note
) {

    public static UpdateAssessmentStatusCommand toCommand(UUID assessmentId, UpdateAssessmentStatusRequest request) {
        return new UpdateAssessmentStatusCommand(assessmentId, request.action(), request.note());
    }
}

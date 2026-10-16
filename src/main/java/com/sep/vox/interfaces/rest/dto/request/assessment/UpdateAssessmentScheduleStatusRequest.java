package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateAssessmentScheduleStatusCommand;

import jakarta.validation.constraints.NotBlank;

public record UpdateAssessmentScheduleStatusRequest(
    @NotBlank(message = "Hành động là bắt buộc")
    String action,

    String note,

    UUID targetScheduleId
) {

    public static UpdateAssessmentScheduleStatusCommand toCommand(UUID assessmentId, UUID scheduleId, UpdateAssessmentScheduleStatusRequest request) {
        return new UpdateAssessmentScheduleStatusCommand(
            assessmentId,
            scheduleId,
            request.action(),
            request.note(),
            request.targetScheduleId()
        );
    }
}

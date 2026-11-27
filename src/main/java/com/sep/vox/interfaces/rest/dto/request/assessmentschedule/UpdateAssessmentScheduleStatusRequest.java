package com.sep.vox.interfaces.rest.dto.request.assessmentschedule;

import com.sep.vox.application.port.input.command.assessmentschedule.UpdateAssessmentScheduleStatusCommand;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public record UpdateAssessmentScheduleStatusRequest(
    @NotBlank(message = "Action is required")
    String action,

    String note,

    UUID targetScheduleId
) {

    public static UpdateAssessmentScheduleStatusCommand toCommand(UUID assessmentId, UUID scheduleId,
            UpdateAssessmentScheduleStatusRequest request) {
        return new UpdateAssessmentScheduleStatusCommand(
            assessmentId,
            scheduleId,
            request.action(),
            request.note(),
            request.targetScheduleId()
        );
    }
}

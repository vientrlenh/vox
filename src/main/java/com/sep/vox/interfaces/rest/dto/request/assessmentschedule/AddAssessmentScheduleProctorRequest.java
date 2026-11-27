package com.sep.vox.interfaces.rest.dto.request.assessmentschedule;

import com.sep.vox.application.port.input.command.assessmentschedule.AddAssessmentScheduleProctorCommand;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AddAssessmentScheduleProctorRequest(
    @NotNull(message = "Teacher is required")
    UUID teacherId
) {

    public static AddAssessmentScheduleProctorCommand toCommand(UUID assessmentId, UUID scheduleId,
            AddAssessmentScheduleProctorRequest request) {
        return new AddAssessmentScheduleProctorCommand(assessmentId, scheduleId, request.teacherId());
    }
}

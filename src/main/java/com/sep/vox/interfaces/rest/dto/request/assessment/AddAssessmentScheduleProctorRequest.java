package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.UUID;

import com.sep.vox.application.port.input.command.AddAssessmentScheduleProctorCommand;

import jakarta.validation.constraints.NotNull;

public record AddAssessmentScheduleProctorRequest(
    @NotNull(message = "Giáo viên là bắt buộc")
    UUID teacherId
) {

    public static AddAssessmentScheduleProctorCommand toCommand(UUID assessmentId, UUID scheduleId, AddAssessmentScheduleProctorRequest request) {
        return new AddAssessmentScheduleProctorCommand(assessmentId, scheduleId, request.teacherId());
    }
}

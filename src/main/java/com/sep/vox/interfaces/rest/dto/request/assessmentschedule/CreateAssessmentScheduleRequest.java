package com.sep.vox.interfaces.rest.dto.request.assessmentschedule;

import com.sep.vox.application.port.input.command.assessmentschedule.CreateAssessmentScheduleCommand;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateAssessmentScheduleRequest(
    @NotNull(message = "Room is required")
    UUID schoolRoomId,

    @NotNull(message = "Start time is required")
    Instant startDate,

    @NotNull(message = "End time is required")
    Instant endDate
) {

    public static CreateAssessmentScheduleCommand toCommand(UUID assessmentId, CreateAssessmentScheduleRequest request) {
        return new CreateAssessmentScheduleCommand(
            assessmentId,
            request.schoolRoomId(),
            request.startDate(),
            request.endDate()
        );
    }
}

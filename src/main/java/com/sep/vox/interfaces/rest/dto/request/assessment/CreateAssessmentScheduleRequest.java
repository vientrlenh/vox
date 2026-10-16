package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateAssessmentScheduleCommand;

import jakarta.validation.constraints.NotNull;

public record CreateAssessmentScheduleRequest(
    @NotNull(message = "Phòng học là bắt buộc")
    UUID schoolRoomId,

    @NotNull(message = "Thời gian bắt đầu là bắt buộc")
    Instant startDate,

    @NotNull(message = "Thời gian kết thúc là bắt buộc")
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

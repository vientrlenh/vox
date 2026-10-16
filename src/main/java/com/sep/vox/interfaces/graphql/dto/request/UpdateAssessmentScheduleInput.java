package com.sep.vox.interfaces.graphql.dto.request;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateAssessmentScheduleCommand;

public record UpdateAssessmentScheduleInput(
    UUID schoolRoomId,
    Instant startDate,
    Instant endDate
) {

    public static UpdateAssessmentScheduleCommand toCommand(UUID id, UpdateAssessmentScheduleInput input) {
        if (input == null) {
            return new UpdateAssessmentScheduleCommand(id, null, null, null);
        }
        return new UpdateAssessmentScheduleCommand(
            id,
            input.schoolRoomId(),
            input.startDate(),
            input.endDate()
        );
    }
}

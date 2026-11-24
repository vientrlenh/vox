package com.sep.vox.interfaces.graphql.input.assessmentschedule;

import java.time.Instant;
import java.util.UUID;

import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.assessmentschedule.UpdateAssessmentScheduleCommand;

public record UpdateAssessmentScheduleInput(
    ArgumentValue<UUID> schoolRoomId,
    ArgumentValue<Instant> startDate,
    ArgumentValue<Instant> endDate
) {

    public static UpdateAssessmentScheduleCommand toCommand(UUID id, UpdateAssessmentScheduleInput input) {
        if (input == null) {
            return new UpdateAssessmentScheduleCommand(id, null, false, null, null);
        }
        return new UpdateAssessmentScheduleCommand(
            id,
            input.schoolRoomId().value(), !input.schoolRoomId().isOmitted(),
            input.startDate().value(),
            input.endDate().value());
    }
}

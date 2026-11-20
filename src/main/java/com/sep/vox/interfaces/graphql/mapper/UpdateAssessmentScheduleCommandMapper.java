package com.sep.vox.interfaces.graphql.mapper;

import java.util.UUID;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.UpdateAssessmentScheduleCommand;
import com.sep.vox.interfaces.graphql.dto.request.UpdateAssessmentScheduleInput;

public final class UpdateAssessmentScheduleCommandMapper {

    private UpdateAssessmentScheduleCommandMapper() {
    }

    public static UpdateAssessmentScheduleCommand fromRequest(UUID id, UpdateAssessmentScheduleInput input) {
        if (input == null) {
            return new UpdateAssessmentScheduleCommand(id, null, null, null);
        }
        return new UpdateAssessmentScheduleCommand(
            id, 
            input.schoolRoomId(), 
            DateMapper.toInstant(input.startDate()), 
            DateMapper.toInstant(input.endDate())
        );
    }
}

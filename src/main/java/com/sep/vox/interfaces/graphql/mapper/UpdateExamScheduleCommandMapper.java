package com.sep.vox.interfaces.graphql.mapper;

import java.util.UUID;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.UpdateExamScheduleCommand;
import com.sep.vox.interfaces.graphql.dto.request.UpdateExamScheduleInput;

public final class UpdateExamScheduleCommandMapper {

    private UpdateExamScheduleCommandMapper() {
    }

    public static UpdateExamScheduleCommand fromRequest(UUID id, UpdateExamScheduleInput input) {
        if (input == null) {
            return new UpdateExamScheduleCommand(id, null, null, null);
        }
        return new UpdateExamScheduleCommand(
            id, 
            input.schoolRoomId(), 
            DateMapper.toInstant(input.startDate()), 
            DateMapper.toInstant(input.endDate())
        );
    }
}

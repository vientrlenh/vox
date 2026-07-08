package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateExamScheduleCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateExamScheduleRequest;

public final class CreateExamScheduleCommandMapper {

    private CreateExamScheduleCommandMapper() {
    }

    public static CreateExamScheduleCommand fromRequest(UUID examId, CreateExamScheduleRequest request) {
        return new CreateExamScheduleCommand(
            examId,
            request.schoolRoomId(),
            request.startDate(),
            request.endDate()
        );
    }
}

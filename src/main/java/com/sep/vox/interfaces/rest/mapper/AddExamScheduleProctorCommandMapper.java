package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.AddExamScheduleProctorCommand;
import com.sep.vox.interfaces.rest.dto.request.AddExamScheduleProctorRequest;

public final class AddExamScheduleProctorCommandMapper {

    private AddExamScheduleProctorCommandMapper() {
    }

    public static AddExamScheduleProctorCommand fromRequest(UUID examId, UUID scheduleId,
            AddExamScheduleProctorRequest request) {
        return new AddExamScheduleProctorCommand(examId, scheduleId, request.teacherId());
    }
}

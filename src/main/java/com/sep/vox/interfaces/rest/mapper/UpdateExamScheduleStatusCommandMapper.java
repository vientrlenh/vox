package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateExamScheduleStatusCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamScheduleStatusRequest;

public final class UpdateExamScheduleStatusCommandMapper {

    private UpdateExamScheduleStatusCommandMapper() {
    }

    public static UpdateExamScheduleStatusCommand fromRequest(UUID examId, UUID scheduleId,
            UpdateExamScheduleStatusRequest request) {
        return new UpdateExamScheduleStatusCommand(
            examId,
            scheduleId,
            request.action(),
            request.note(),
            request.targetScheduleId()
        );
    }
}

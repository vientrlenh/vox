package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.CreateAssessmentScheduleCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateAssessmentScheduleRequest;

public final class CreateAssessmentScheduleCommandMapper {

    private CreateAssessmentScheduleCommandMapper() {
    }

    public static CreateAssessmentScheduleCommand fromRequest(UUID assessmentId, CreateAssessmentScheduleRequest request) {
        return new CreateAssessmentScheduleCommand(
            assessmentId,
            request.schoolRoomId(),
            DateMapper.toInstant(request.startDate()),
            DateMapper.toInstant(request.endDate())
        );
    }
}

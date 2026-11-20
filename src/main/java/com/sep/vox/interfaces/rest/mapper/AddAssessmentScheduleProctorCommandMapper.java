package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.AddAssessmentScheduleProctorCommand;
import com.sep.vox.interfaces.rest.dto.request.AddAssessmentScheduleProctorRequest;

public final class AddAssessmentScheduleProctorCommandMapper {

    private AddAssessmentScheduleProctorCommandMapper() {
    }

    public static AddAssessmentScheduleProctorCommand fromRequest(UUID assessmentId, UUID scheduleId,
            AddAssessmentScheduleProctorRequest request) {
        return new AddAssessmentScheduleProctorCommand(assessmentId, scheduleId, request.teacherId());
    }
}

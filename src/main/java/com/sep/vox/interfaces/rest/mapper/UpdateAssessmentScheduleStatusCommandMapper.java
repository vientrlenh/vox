package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateAssessmentScheduleStatusCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateAssessmentScheduleStatusRequest;

public final class UpdateAssessmentScheduleStatusCommandMapper {

    private UpdateAssessmentScheduleStatusCommandMapper() {
    }

    public static UpdateAssessmentScheduleStatusCommand fromRequest(UUID assessmentId, UUID scheduleId,
            UpdateAssessmentScheduleStatusRequest request) {
        return new UpdateAssessmentScheduleStatusCommand(
            assessmentId,
            scheduleId,
            request.action(),
            request.note(),
            request.targetScheduleId()
        );
    }
}

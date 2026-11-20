package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateAssessmentDeliveryModeCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateAssessmentDeliveryModeRequest;

public final class UpdateAssessmentDeliveryModeCommandMapper {

    private UpdateAssessmentDeliveryModeCommandMapper() {
    }

    public static UpdateAssessmentDeliveryModeCommand fromRequest(UUID assessmentId, UpdateAssessmentDeliveryModeRequest request) {
        return new UpdateAssessmentDeliveryModeCommand(assessmentId, request.deliveryMode());
    }
}

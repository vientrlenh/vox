package com.sep.vox.interfaces.rest.dto.request.assessment;

import com.sep.vox.application.port.input.command.assessment.UpdateAssessmentDeliveryModeCommand;
import com.sep.vox.domain.model.assessment.AssessmentDeliveryMode;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record UpdateAssessmentDeliveryModeRequest(
    @NotNull(message = "Hình thức làm bài là bắt buộc")
    AssessmentDeliveryMode deliveryMode
) {

    public static UpdateAssessmentDeliveryModeCommand toCommand(UUID assessmentId, UpdateAssessmentDeliveryModeRequest request) {
        return new UpdateAssessmentDeliveryModeCommand(assessmentId, request.deliveryMode());
    }
}

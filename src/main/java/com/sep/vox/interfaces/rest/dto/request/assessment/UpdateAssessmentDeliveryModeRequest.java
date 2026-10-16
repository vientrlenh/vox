package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateAssessmentDeliveryModeCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateAssessmentDeliveryModeRequest(
    @NotBlank(message = "Hình thức làm bài là bắt buộc")
    @Pattern(
        regexp = "STUDENT_DEVICE|LAB",
        message = "Hình thức làm bài không hợp lệ"
    )
    String deliveryMode
) {

    public static UpdateAssessmentDeliveryModeCommand toCommand(UUID assessmentId, UpdateAssessmentDeliveryModeRequest request) {
        return new UpdateAssessmentDeliveryModeCommand(assessmentId, request.deliveryMode());
    }
}

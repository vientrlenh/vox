package com.sep.vox.interfaces.rest.dto.request;

import com.sep.vox.domain.model.assessment.AssessmentDeliveryMode;

import jakarta.validation.constraints.NotNull;

public record UpdateAssessmentDeliveryModeRequest(
    @NotNull(message = "Hình thức làm bài là bắt buộc")
    AssessmentDeliveryMode deliveryMode
) {
}

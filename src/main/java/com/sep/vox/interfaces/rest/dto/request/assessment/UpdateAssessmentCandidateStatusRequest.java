package com.sep.vox.interfaces.rest.dto.request.assessment;

import jakarta.validation.constraints.NotBlank;

public record UpdateAssessmentCandidateStatusRequest(
    @NotBlank(message = "Trạng thái là bắt buộc")
    String status
) {
}

package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateExamCandidateStatusRequest(
    @NotBlank(message = "Trạng thái là bắt buộc")
    String status
) {
}

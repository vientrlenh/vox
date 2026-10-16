package com.sep.vox.interfaces.rest.dto.request.assessment;

import jakarta.validation.constraints.NotBlank;

public record UpdateExamSessionRequest(
    @NotBlank String status
) {
}

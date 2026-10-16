package com.sep.vox.interfaces.rest.dto.request.assessment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateAssessmentBlueprintVersionStatusRequest(
    @NotBlank(message = "Action là bắt buộc")
    @Pattern(regexp = "PUBLISH|ARCHIVE", message = "Action không hợp lệ")
    String action,
    String note
) {
}

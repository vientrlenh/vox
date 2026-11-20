package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateExamBlueprintVersionStatusRequest(
    @NotBlank(message = "Action là bắt buộc")
    @Pattern(regexp = "PUBLISH|ARCHIVE", message = "Action không hợp lệ")
    String action,
    String note
) {
}

package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateExamStatusRequest(
    @NotBlank(message = "Action là bắt buộc")
    @Pattern(
        regexp = "SCHEDULE|START|CLOSE|PUBLISH_RESULTS|CANCEL",
        message = "Action không hợp lệ"
    )
    String action,
    String note
) {
}

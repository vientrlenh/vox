package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateQuestionStatusRequest(
    @NotBlank(message = "Action là bắt buộc")
    @Pattern(
        regexp = "SUBMIT|APPROVE|REJECT|REQUEST_REVISION|PUBLISH|ARCHIVE|LOCK|UNLOCK",
        message = "Action không hợp lệ"
    )
    String action,
    String note
) {
}

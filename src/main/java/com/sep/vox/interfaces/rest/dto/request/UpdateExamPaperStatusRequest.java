package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateExamPaperStatusRequest(
    @NotBlank(message = "Action là bắt buộc")
    @Pattern(
        regexp = "SUBMIT|APPROVE|REQUEST_REVISION|LOCK|REOPEN",
        message = "Action không hợp lệ"
    )
    String action,
    String note
) {
}

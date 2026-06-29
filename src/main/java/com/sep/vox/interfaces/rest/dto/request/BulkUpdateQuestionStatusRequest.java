package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

public record BulkUpdateQuestionStatusRequest(
    @NotEmpty(message = "Danh sách câu hỏi không được để trống")
    List<UUID> questionIds,

    @NotBlank(message = "Action là bắt buộc")
    @Pattern(
        regexp = "SUBMIT|APPROVE|REJECT|REQUEST_REVISION|PUBLISH|ARCHIVE|REOPEN|LOCK|UNLOCK",
        message = "Action không hợp lệ"
    )
    String action,
    String note
) {
}

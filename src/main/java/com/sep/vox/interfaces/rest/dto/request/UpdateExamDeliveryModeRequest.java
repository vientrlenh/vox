package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateExamDeliveryModeRequest(
    @NotBlank(message = "Hình thức làm bài là bắt buộc")
    @Pattern(
        regexp = "STUDENT_DEVICE|LAB",
        message = "Hình thức làm bài không hợp lệ"
    )
    String deliveryMode
) {
}

package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.Min;

public record CheckpointExamSessionRemainingTimeRequest(
    @Min(value = 0, message = "Thời gian còn lại không được nhỏ hơn 0")
    int remainingSeconds
) {
}

package com.sep.vox.interfaces.rest.dto.request;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;

public record ApproveExamAppealRequest(
    @NotNull(message = "Phải đặt hạn xử lý cho đơn phúc khảo")
    OffsetDateTime deadline
) {
}

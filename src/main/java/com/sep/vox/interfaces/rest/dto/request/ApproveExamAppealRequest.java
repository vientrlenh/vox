package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotNull;

public record ApproveExamAppealRequest(
    @NotNull(message = "Phải đặt hạn xử lý cho đơn phúc khảo")
    String deadline
) {
}

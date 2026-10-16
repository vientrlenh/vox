package com.sep.vox.interfaces.rest.dto.request.assessment;

import jakarta.validation.constraints.NotNull;

public record ApproveAssessmentAppealRequest(
    @NotNull(message = "Phải đặt hạn xử lý cho đơn phúc khảo")
    String deadline
) {
}

package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * `partScore` là điểm cho phần thi được phúc khảo, KHÔNG phải điểm tổng.
 * Điểm tổng và result band do hệ thống tính lại từ điểm này.
 */
public record PublishExamAppealRequest(
    @NotNull(message = "Phải nhập điểm cho phần thi được phúc khảo")
    BigDecimal partScore,

    @Size(max = 512, message = "Ghi chú công bố tối đa 512 ký tự")
    String decisionNote
) {
}

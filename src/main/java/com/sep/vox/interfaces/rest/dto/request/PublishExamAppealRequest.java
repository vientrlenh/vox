package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * `partScore` là điểm cho từng phần thi được phúc khảo, KHÔNG phải điểm tổng.
 * Điểm tổng và result band do hệ thống tính lại từ các điểm này.
 */
public record PublishExamAppealRequest(
    @NotEmpty(message = "Phải nhập điểm cho tất cả phần thi được phúc khảo")
    @Valid
    List<ItemScoreRequest> itemScores,

    @Size(max = 512, message = "Ghi chú công bố tối đa 512 ký tự")
    String decisionNote
) {
    public record ItemScoreRequest(
        @NotNull(message = "Thiếu phần thi cần công bố điểm")
        UUID appealItemId,

        @NotNull(message = "Phải nhập điểm cho phần thi")
        BigDecimal partScore
    ) {
    }
}

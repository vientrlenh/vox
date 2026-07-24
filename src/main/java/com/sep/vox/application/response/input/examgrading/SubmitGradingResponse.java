package com.sep.vox.application.response.input.examgrading;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * {@code totalScore} và {@code resultStatus} là thứ BE tính lại được sau khi ghi,
 * không phải thứ giáo viên gửi lên.
 */
public record SubmitGradingResponse(
    UUID candidateResultId,
    BigDecimal totalScore,
    String resultStatus
) {
}

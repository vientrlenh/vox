package com.sep.vox.application.response.input.examgrading;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Kết quả chấm thử. Phải khớp tuyệt đối với tổng mà {@code /grade} trả về khi nộp
 * cùng bộ điểm — cả hai đi qua {@code ExamSessionResultCalculator}.
 */
public record GradingPreviewResponse(
    BigDecimal totalScore,
    String resultBandName,
    List<ItemScore> itemScores
) {
    public record ItemScore(
        UUID paperItemId,
        BigDecimal itemScore
    ) {
    }
}

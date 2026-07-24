package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * `partScore` là điểm cho từng part được phúc khảo, KHÔNG phải điểm tổng.
 * Tổng và result band do ExamSessionResultCalculator dẫn xuất lại từ các điểm này.
 */
public record PublishExamAppealCommand(
    UUID appealId,
    List<ItemScore> itemScores,
    String decisionNote
) {
    public record ItemScore(
        UUID appealItemId,
        BigDecimal partScore
    ) {
    }
}

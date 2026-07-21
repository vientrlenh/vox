package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * `partScore` là điểm cho part được phúc khảo, KHÔNG phải điểm tổng.
 * Tổng và result band do ExamSessionResultCalculator dẫn xuất lại từ điểm này.
 */
public record PublishExamAppealCommand(
    UUID appealId,
    BigDecimal partScore,
    String decisionNote
) {
}

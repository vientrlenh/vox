package com.sep.vox.application.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Kết quả được chốt {@code PASSED}/{@code FAILED} — mốc cuối cùng trong vòng đời điểm
 * của một bài.
 */
public record ExamResultOutcomeDecidedEvent(
    UUID candidateResultId,
    UUID studentId,
    String examName,
    String outcome,
    BigDecimal totalScore
) {
}

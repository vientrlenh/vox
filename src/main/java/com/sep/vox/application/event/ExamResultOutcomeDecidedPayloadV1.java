package com.sep.vox.application.event;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamKind;

/**
 * Kết quả được chốt {@code PASSED}/{@code FAILED} — mốc cuối cùng trong vòng đời điểm
 * của một bài.
 *
 * @param sessionId màn hình kết quả của học sinh nhận sessionId, không phải candidateResultId
 * @param examKind bài tập trung và bài kiểm tra lớp có hai màn hình kết quả riêng
 */
public record ExamResultOutcomeDecidedPayloadV1(
    UUID candidateResultId,
    UUID studentId,
    String examName,
    String outcome,
    BigDecimal totalScore,
    UUID sessionId,
    ExamKind examKind
) {

}

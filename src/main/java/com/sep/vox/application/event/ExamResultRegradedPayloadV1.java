package com.sep.vox.application.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Điểm ĐÃ CÔNG BỐ nay thay đổi (hậu kiểm hoặc phúc khảo chấm lại).
 *
 * <p>Chỉ phát khi điểm thật sự đổi: hậu kiểm rồi giữ nguyên không được làm phiền học
 * sinh bằng một mail "điểm của em vừa thay đổi" trong khi nó không đổi.
 *
 * @param roundType vòng đã sinh ra thay đổi — mail nói rõ do hậu kiểm hay phúc khảo
 */
public record ExamResultRegradedPayloadV1(
    UUID candidateResultId,
    UUID studentId,
    String examName,
    String roundType,
    BigDecimal scoreBefore,
    BigDecimal scoreAfter
) {

}

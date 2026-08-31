package com.sep.vox.application.event;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamKind;

/**
 * Điểm ĐÃ CÔNG BỐ nay thay đổi (hậu kiểm hoặc phúc khảo chấm lại).
 *
 * <p>Chỉ phát khi điểm thật sự đổi: hậu kiểm rồi giữ nguyên không được làm phiền học
 * sinh bằng một mail "điểm của em vừa thay đổi" trong khi nó không đổi.
 *
 * @param roundType vòng đã sinh ra thay đổi — mail nói rõ do hậu kiểm hay phúc khảo
 *
 * @param sessionId màn hình kết quả của học sinh nhận sessionId, không phải candidateResultId
 * @param examKind bài tập trung và bài kiểm tra lớp có hai màn hình kết quả riêng
 */
public record ExamResultRegradedPayloadV1(
    UUID candidateResultId,
    UUID studentId,
    String examName,
    String roundType,
    BigDecimal scoreBefore,
    BigDecimal scoreAfter,
    UUID sessionId,
    ExamKind examKind
) {

}

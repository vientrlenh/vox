package com.sep.vox.application.event;

import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamKind;

/**
 * Bài được gỡ vô hiệu sau khi soi lại — sẽ được chấm lại từ đầu.
 *
 * @param sessionId màn hình kết quả của học sinh nhận sessionId, không phải candidateResultId
 * @param examKind bài tập trung và bài kiểm tra lớp có hai màn hình kết quả riêng
 */
public record ExamResultInvalidClearedPayloadV1(
    UUID candidateResultId,
    UUID studentId,
    String examName,
    String reason,
    UUID sessionId,
    ExamKind examKind
) {

}

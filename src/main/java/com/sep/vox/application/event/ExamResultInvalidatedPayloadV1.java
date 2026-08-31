package com.sep.vox.application.event;

import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamKind;

/**
 * Bài bị kết luận vi phạm -> vô hiệu. Lý do là bắt buộc nên luôn có nội dung để gửi.
 *
 * @param sessionId màn hình kết quả của học sinh nhận sessionId, không phải candidateResultId
 * @param examKind bài tập trung và bài kiểm tra lớp có hai màn hình kết quả riêng
 */
public record ExamResultInvalidatedPayloadV1(
    UUID candidateResultId,
    UUID studentId,
    String examName,
    String reason,
    UUID sessionId,
    ExamKind examKind
) {

}

package com.sep.vox.application.event;

import java.util.UUID;

/** Bài bị kết luận vi phạm -> vô hiệu. Lý do là bắt buộc nên luôn có nội dung để gửi. */
public record ExamResultInvalidatedPayloadV1(
    UUID candidateResultId,
    UUID studentId,
    String examName,
    String reason
) {

}

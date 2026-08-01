package com.sep.vox.application.event;

import java.util.UUID;

/** Bài được gỡ vô hiệu sau khi soi lại — sẽ được chấm lại từ đầu. */
public record ExamResultInvalidClearedPayloadV1(
    UUID candidateResultId,
    UUID studentId,
    String examName,
    String reason
) {

}

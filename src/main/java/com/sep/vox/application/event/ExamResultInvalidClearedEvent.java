package com.sep.vox.application.event;

import java.util.UUID;

/** Bài được gỡ vô hiệu sau khi soi lại — sẽ được chấm lại từ đầu. */
public record ExamResultInvalidClearedEvent(
    UUID candidateResultId,
    UUID studentId,
    String examName,
    String reason
) {
}

package com.sep.vox.application.event;

import java.time.Instant;
import java.util.UUID;

/** Nhắc giáo viên một phân công sắp/đã tới hạn. Mỗi phân công chỉ nhắc một lần. */
public record GradingDeadlineReminderPayloadV1(
    UUID assignmentId,
    UUID teacherId,
    String examName,
    String roundType,
    Instant deadlineAt
) {

}

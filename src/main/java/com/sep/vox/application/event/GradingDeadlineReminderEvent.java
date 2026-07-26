package com.sep.vox.application.event;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Nhắc giáo viên một phân công sắp/đã tới hạn. Mỗi phân công chỉ nhắc một lần. */
public record GradingDeadlineReminderEvent(
    UUID assignmentId,
    UUID teacherId,
    String examName,
    String roundType,
    OffsetDateTime deadlineAt
) {
}

package com.sep.vox.application.port.input.query;

import java.util.UUID;

/** Phân trang 0-based, đồng bộ với các query cùng domain exam-appeal. */
public record SearchGradingAssignmentsQuery(
    UUID examId,
    UUID scheduleId,
    UUID teacherId,
    String status,
    String search,
    int page,
    int size
) {
}

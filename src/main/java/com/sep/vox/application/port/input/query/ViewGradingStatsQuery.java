package com.sep.vox.application.port.input.query;

import java.util.UUID;

/**
 * @param kind loại bài; {@code null} được use case hiểu là {@code CENTRALIZED} — cùng
 *             luật với {@link SearchGradingAssignmentsQuery} để bảng và thẻ số đầu màn
 *             không bao giờ đếm trên hai tập khác nhau.
 */
public record ViewGradingStatsQuery(
    UUID examId,
    UUID scheduleId,
    String kind
) {
}

package com.sep.vox.application.query.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * An attempt whose AI grading failed that NOBODY has handled yet, seen from the school's side.
 *
 * <p>No score: a {@code GRADING_FAILED} session never produced an {@code assessment_candidate_results}
 * row. What replaces it are the fields that help decide between the two exits -- ask the AI to regrade or assign a
 * teacher to grade by hand.
 *
 * <p>Two DIFFERENT counters, do not merge them:
 *
 * <ul>
 *   <li>{@code aiRetryCount} is the number of times the grading service retried by itself inside ONE attempt, taken from the Kafka
 *       payload. It speaks to how persistent the incident is, not to who pressed what.
 *   <li>{@code schoolRetryLeft} is derived from {@code assessment_sessions.school_regrade_count} -- the school's allowance of
 *       one AI regrade request. This is what decides which button can still be pressed.
 * </ul>
 */
public record SchoolGradingFailureDto(
    UUID sessionId,
    UUID assessmentId,
    String assessmentCode,
    String assessmentName,
    String candidateName,
    /** null khi học sinh chưa được xếp vào lớp đang hoạt động nào. */
    String className,
    /** Mốc nộp bài — phiên hỏng không có mốc nào khác đáng tin để xếp thứ tự. */
    Instant failedAt,
    /** Thông điệp lỗi thô; null với phiên bị đánh dấu hỏng qua nhánh DLT. */
    String error,
    /** Dịch vụ chấm đã tự thử mấy lần trước khi bỏ cuộc; null khi không rõ. */
    Integer aiRetryCount,
    /** Trường còn lượt nhờ AI chấm lại cho phiên này hay không. */
    boolean schoolRetryLeft
) {
}

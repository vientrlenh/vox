package com.sep.vox.application.event;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamKind;

/**
 * Nhắc giáo viên một phân công sắp/đã tới hạn. Mỗi phân công chỉ nhắc một lần.
 *
 * @param examId màn hình chấm của bài kiểm tra lớp nằm dưới examId, bài tập trung thì không
 * @param examKind quyết định mở màn hình chấm nào
 */
public record GradingDeadlineReminderPayloadV1(
    UUID assignmentId,
    UUID teacherId,
    String examName,
    String roundType,
    Instant deadlineAt,
    UUID examId,
    ExamKind examKind
) {

}

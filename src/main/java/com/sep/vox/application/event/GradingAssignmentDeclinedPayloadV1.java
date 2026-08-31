package com.sep.vox.application.event;

import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamKind;

/**
 * Giáo viên trả lại phân công. Người nhận tin là <em>admin đã giao</em>, không phải
 * học sinh: bài quay về hàng chưa giao và cần người điều phối xử lý tiếp.
 *
 * @param examId màn hình chấm của bài kiểm tra lớp nằm dưới examId, bài tập trung thì không
 * @param examKind quyết định mở màn hình chấm nào
 */
public record GradingAssignmentDeclinedPayloadV1(
    UUID assignmentId,
    UUID candidateResultId,
    UUID teacherId,
    UUID assignedBy,
    String examName,
    String reason,
    UUID examId,
    ExamKind examKind
) {

}

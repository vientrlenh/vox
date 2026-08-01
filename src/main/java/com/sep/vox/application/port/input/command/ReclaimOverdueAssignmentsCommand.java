package com.sep.vox.application.port.input.command;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Thu hồi các phân công quá hạn và (tuỳ chọn) giao lại ngay cho nhóm khác.
 *
 * @param assignmentIds bỏ trống = thu hồi mọi phân công quá hạn trong phạm vi
 *        {@code examId}; có giá trị = chỉ đúng các dòng được chọn
 * @param reassignToTeacherIds bỏ trống = chỉ thu hồi, bài về hàng chưa giao
 */
public record ReclaimOverdueAssignmentsCommand(
    UUID examId,
    List<UUID> assignmentIds,
    List<UUID> reassignToTeacherIds,
    Instant newDeadlineAt
) {
}

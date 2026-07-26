package com.sep.vox.interfaces.rest.dto.request;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * @param assignmentIds bỏ trống = thu hồi mọi phân công quá hạn trong phạm vi
 *        {@code examId} (bỏ trống cả hai = toàn trường)
 * @param reassignToTeacherIds bỏ trống = chỉ thu hồi, bài về hàng chưa giao
 */
public record ReclaimOverdueAssignmentsRequest(
    UUID examId,
    List<UUID> assignmentIds,
    List<UUID> reassignToTeacherIds,
    OffsetDateTime newDeadlineAt
) {
}

package com.sep.vox.interfaces.rest.dto.request;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.ReclaimOverdueAssignmentsCommand;

/**
 * @param assignmentIds bỏ trống = thu hồi mọi phân công quá hạn trong phạm vi
 *        {@code assessmentId} (bỏ trống cả hai = toàn trường)
 * @param reassignToTeacherIds bỏ trống = chỉ thu hồi, bài về hàng chưa giao
 */
public record ReclaimOverdueAssignmentsRequest(
    UUID assessmentId,
    List<UUID> assignmentIds,
    List<UUID> reassignToTeacherIds,
    Instant newDeadlineAt
) {

    public static ReclaimOverdueAssignmentsCommand toCommand(ReclaimOverdueAssignmentsRequest request) {
        return new ReclaimOverdueAssignmentsCommand(
            request.assessmentId(),
            request.assignmentIds(),
            request.reassignToTeacherIds(),
            request.newDeadlineAt()
        );
    }
}

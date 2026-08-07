package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

/** {@code scheduleId} cố ý không bắt buộc: để trống nghĩa là gỡ cả nhóm khỏi ca thi. */
public record BulkAssignExamCandidateScheduleRequest(
    @NotEmpty(message = "Danh sách thí sinh không được để trống")
    List<UUID> candidateIds,
    UUID scheduleId
) {
}

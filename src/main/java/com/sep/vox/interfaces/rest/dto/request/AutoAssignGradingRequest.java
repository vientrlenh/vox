package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

/**
 * Phải có {@code examId} hoặc {@code scheduleId} — ràng buộc "ít nhất một" nằm ở
 * use case vì Bean Validation không diễn đạt được nó gọn hơn.
 */
public record AutoAssignGradingRequest(
    UUID examId,

    UUID scheduleId,

    @NotEmpty(message = "Phải chọn ít nhất một giáo viên để phân công")
    List<UUID> teacherIds
) {
}

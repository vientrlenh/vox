package com.sep.vox.interfaces.rest.dto.request;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

/**
 * @param deadlineAt bỏ trống = gỡ hạn cho các phân công được chọn (bài không còn bị
 *        tính là quá hạn)
 */
public record SetGradingDeadlineRequest(
    @NotEmpty(message = "Phải chọn ít nhất một phân công để đặt hạn")
    List<UUID> assignmentIds,

    Instant deadlineAt
) {
}

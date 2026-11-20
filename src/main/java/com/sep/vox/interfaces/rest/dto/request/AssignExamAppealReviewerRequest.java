package com.sep.vox.interfaces.rest.dto.request;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Giao MỘT người chấm phúc khảo.
 *
 * @param overrideReason bỏ trống = giữ luật xung đột lợi ích (người đã từng chấm bài
 *        này sẽ bị từ chối). Có giá trị = admin chấp nhận giao cho họ, và lý do được
 *        lưu lại trên đơn.
 */
public record AssignExamAppealReviewerRequest(
    @NotNull(message = "Thiếu người chấm phúc khảo")
    UUID reviewerId,

    @Size(max = 1024, message = "Lý do tối đa 1024 ký tự")
    String overrideReason,

    Instant deadlineAt
) {
}

package com.sep.vox.application.port.input.command;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Đặt / sửa hạn chấm cho một hoặc nhiều phân công.
 *
 * @param deadlineAt {@code null} = gỡ hạn (bài không còn bị tính quá hạn)
 */
public record SetGradingDeadlineCommand(
    List<UUID> assignmentIds,
    OffsetDateTime deadlineAt
) {
}

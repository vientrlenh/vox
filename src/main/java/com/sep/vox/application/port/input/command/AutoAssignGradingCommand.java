package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

/**
 * Auto-assign round-robin trong phạm vi một kỳ thi hoặc một ca thi. Chỉ nhận bài
 * đang PENDING_REVIEW và chưa có phân công.
 */
public record AutoAssignGradingCommand(
    UUID examId,
    UUID scheduleId,
    List<UUID> teacherIds
) {
}

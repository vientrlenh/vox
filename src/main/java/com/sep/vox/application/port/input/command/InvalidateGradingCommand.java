package com.sep.vox.application.port.input.command;

import java.util.UUID;

/**
 * Giáo viên (hoặc nhà trường) xác nhận bài flagged là vi phạm thật -> result INVALID,
 * không nhập điểm. Đúng một trong assignmentId/candidateResultId khác null -- xem
 * {@link SubmitGradingCommand}.
 */
public record InvalidateGradingCommand(
    UUID assignmentId,
    UUID candidateResultId,
    String reason
) {
}

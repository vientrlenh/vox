package com.sep.vox.application.port.input.command;

import java.util.UUID;

/** Giáo viên xác nhận bài flagged là vi phạm thật -> result INVALID, không nhập điểm. */
public record InvalidateGradingCommand(
    UUID assignmentId,
    String reason
) {
}

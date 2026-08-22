package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

/** Dùng chung cho cả ngân hàng và chủ đề câu hỏi -- hai luồng có cùng hình dạng đầu vào. */
public record BulkUpdateQuestionScopeStatusCommand(
    List<UUID> ids,
    String action
) {
}

package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record BulkUpdateQuestionStatusCommand(
    List<UUID> questionIds,
    String action,
    String note
) {
}

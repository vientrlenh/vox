package com.sep.vox.application.port.input.command.questionbank;

import java.util.List;
import java.util.UUID;

/** Shared by both question banks and question topics -- the two flows have the same input shape. */
public record BulkUpdateQuestionScopeStatusCommand(
    List<UUID> ids,
    String action
) {
}

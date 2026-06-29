package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateExamStatusCommand(
    UUID examId,
    String action,
    String note
) {
}

package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CompleteExamSessionGradingCommand(
    UUID examSessionId
) {
}
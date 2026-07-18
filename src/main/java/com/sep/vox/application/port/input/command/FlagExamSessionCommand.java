package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record FlagExamSessionCommand(
    UUID sessionId,
    String reason
) {
}

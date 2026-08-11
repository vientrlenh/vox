package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record EndPracticeSessionCommand(
    UUID sessionId,
    int helpRequestCount,
    int longPauseCount
) {
}

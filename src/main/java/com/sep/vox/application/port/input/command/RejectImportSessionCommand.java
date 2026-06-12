package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record RejectImportSessionCommand(
    UUID importSessionId,
    String reason
) {
}

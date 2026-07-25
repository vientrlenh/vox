package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record RejectExamAppealCommand(
    UUID appealId,
    String reason
) {
}

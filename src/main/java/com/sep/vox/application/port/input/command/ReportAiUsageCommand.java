package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record ReportAiUsageCommand(
    UUID assessmentSessionId,
    List<RecordAiUsageCommand> usageEvents
) {
}

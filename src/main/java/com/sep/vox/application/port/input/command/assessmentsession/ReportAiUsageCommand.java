package com.sep.vox.application.port.input.command.assessmentsession;

import com.sep.vox.application.port.input.command.metering.RecordAiUsageCommand;
import java.util.List;
import java.util.UUID;

public record ReportAiUsageCommand(
    UUID assessmentSessionId,
    List<RecordAiUsageCommand> usageEvents
) {
}

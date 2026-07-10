package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record AutoFillExamCandidatesCommand(
    UUID examId,
    List<UUID> scheduleIds
) {
}

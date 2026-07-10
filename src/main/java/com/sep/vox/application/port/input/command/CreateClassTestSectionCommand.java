package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record CreateClassTestSectionCommand(
    UUID examId,
    String title,
    String instruction,
    List<UUID> questionIds
) {
}

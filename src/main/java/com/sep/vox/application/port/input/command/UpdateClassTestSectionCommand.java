package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record UpdateClassTestSectionCommand(
    UUID examId,
    UUID sectionId,
    String title,
    List<UUID> questionIds
) {
}

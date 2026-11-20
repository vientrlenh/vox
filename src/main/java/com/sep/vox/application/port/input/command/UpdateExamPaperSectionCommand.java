package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateExamPaperSectionCommand(
    UUID paperId,
    UUID sectionId,
    String title,
    String instruction
) {
}

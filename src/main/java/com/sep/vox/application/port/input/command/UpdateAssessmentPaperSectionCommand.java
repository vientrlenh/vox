package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateAssessmentPaperSectionCommand(
    UUID paperId,
    UUID sectionId,
    String title,
    String instruction
) {
}

package com.sep.vox.application.port.input.command.assessmentpaper;

import java.util.UUID;

public record UpdateAssessmentPaperStatusCommand(
    UUID paperId,
    String action,
    String note
) {
}

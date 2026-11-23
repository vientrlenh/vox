package com.sep.vox.application.port.input.command.assessmentpaper;

import java.util.UUID;

public record DeleteAssessmentPaperCommand(
    UUID paperId
) {
}

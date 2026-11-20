package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateAssessmentPaperItemCommand(
    UUID paperId,
    UUID itemId,
    UUID questionId
) {
}

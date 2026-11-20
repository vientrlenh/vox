package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateExamPaperItemCommand(
    UUID paperId,
    UUID itemId,
    UUID questionId
) {
}

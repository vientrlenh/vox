package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateExamPaperStatusCommand(
    UUID paperId,
    String action,
    String note
) {
}

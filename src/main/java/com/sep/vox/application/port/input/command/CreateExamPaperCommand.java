package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CreateExamPaperCommand(
    UUID examId,
    String source,
    UUID copyFromPaperId
) {
}

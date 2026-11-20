package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CreateExamSessionCommand(
    UUID examId,
    UUID candidateId,
    UUID paperId
) {
}

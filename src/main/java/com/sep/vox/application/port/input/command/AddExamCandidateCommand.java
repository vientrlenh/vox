package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record AddExamCandidateCommand(
    UUID examId,
    UUID studentId
) {
}

package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record ImportExamCandidatesFromGradeCommand(
    UUID examId,
    UUID schoolGradeId
) {
}

package com.sep.vox.application.port.input.command;

import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamCandidateStatus;

public record UpdateExamCandidateStatusCommand(
    UUID candidateId,
    ExamCandidateStatus status
) {
}

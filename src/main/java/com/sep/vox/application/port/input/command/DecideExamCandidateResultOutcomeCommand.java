package com.sep.vox.application.port.input.command;

import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;

public record DecideExamCandidateResultOutcomeCommand(
    UUID candidateResultId,
    ExamCandidateResultStatus decision
) {
}

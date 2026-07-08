package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamCandidateStatus;

public record ViewExamCandidatesQuery(
    UUID examId,
    UUID scheduleId,
    ExamCandidateStatus status
) {
}

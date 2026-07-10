package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamKind;

public record ViewExamStatusCountsQuery(
    UUID schoolId,
    ExamKind kind
) {
}

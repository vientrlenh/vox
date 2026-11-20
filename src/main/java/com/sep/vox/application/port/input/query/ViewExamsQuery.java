package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamStatus;

public record ViewExamsQuery(
    ExamKind kind,
    ExamStatus status,
    UUID schoolId,
    UUID schoolClassId,
    String keyword,
    int page,
    int size
) {
}

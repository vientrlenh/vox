package com.sep.vox.application.port.input.query;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamScheduleStatus;

public record ViewExamSchedulesQuery(
    UUID examId,
    ExamScheduleStatus status,
    Instant startDate,
    Instant endDate
) {
}

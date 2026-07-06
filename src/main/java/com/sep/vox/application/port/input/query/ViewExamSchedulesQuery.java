package com.sep.vox.application.port.input.query;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamScheduleStatus;

public record ViewExamSchedulesQuery(
    UUID examId,
    ExamScheduleStatus status,
    OffsetDateTime startDate,
    OffsetDateTime endDate
) {
}

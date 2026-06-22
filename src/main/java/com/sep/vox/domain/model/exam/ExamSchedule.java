package com.sep.vox.domain.model.exam;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ExamSchedule {
    private UUID id;
    private UUID examId;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private ExamScheduleStatus status;
    private UUID movedToScheduleId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}

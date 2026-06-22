package com.sep.vox.domain.model.exam;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ExamCandidate {
    private UUID id;
    private UUID examId;
    private UUID studentId;
    private UUID assignedPaperId;
    private UUID scheduleId;
    private ExamCandidateStatus status;
    private OffsetDateTime assignedAt;
    private OffsetDateTime updatedAt;
    private UUID assignedBy;
    private UUID updatedBy;
}

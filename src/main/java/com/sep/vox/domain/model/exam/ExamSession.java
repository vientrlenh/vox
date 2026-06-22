package com.sep.vox.domain.model.exam;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ExamSession {
    private UUID id;
    private UUID examId;
    private UUID candidateId;
    private UUID paperId;
    private OffsetDateTime startedAt;
    private OffsetDateTime submittedAt;
    private ExamSessionStatus status;
}

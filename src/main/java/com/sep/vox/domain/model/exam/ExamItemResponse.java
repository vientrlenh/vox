package com.sep.vox.domain.model.exam;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ExamItemResponse {
    private UUID id;
    private UUID sessionId;
    private UUID paperItemId;
    private String audioUrl;
    private Integer durationSeconds;
    private String transcript; 
    private OffsetDateTime submittedAt;
}

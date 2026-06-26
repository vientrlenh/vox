package com.sep.vox.domain.model.exam;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ExamBlueprintVersion {
    private UUID id;
    private UUID blueprintId;
    private int version;
    private String code;
    private String description;
    private ExamBlueprintVersionStatus status;
    private Integer totalTimeLimitSeconds;
    private OffsetDateTime effectiveFrom;
    private OffsetDateTime effectiveTo;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    
}

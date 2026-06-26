package com.sep.vox.domain.model.exam;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ExamBlueprintSection {
    private UUID id;
    private UUID blueprintVersionId;
    private int order;
    private String title;
    private String instruction;
    private Integer sectionTimeLimitSeconds;
    private BigDecimal sectionWeight;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}

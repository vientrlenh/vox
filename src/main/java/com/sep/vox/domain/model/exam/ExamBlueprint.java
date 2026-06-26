package com.sep.vox.domain.model.exam;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ExamBlueprint {
    private UUID id;
    private UUID schoolId;
    private UUID languageId;
    private UUID schoolGradeLevelId;
    private String code;
    private String name;
    private String description;
    private boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}

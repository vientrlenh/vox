package com.sep.vox.domain.model.exam;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Exam {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private UUID schoolId;
    private UUID languageId;
    private ExamStatus status;
    private OffsetDateTime openAt;
    private OffsetDateTime closeAt;
    private UUID accessmentPolicyId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}
